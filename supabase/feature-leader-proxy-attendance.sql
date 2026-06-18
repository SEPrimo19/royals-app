
ALTER TABLE event_attendance
  ADD COLUMN IF NOT EXISTS posted_by_proxy UUID REFERENCES users(id);

ALTER TABLE event_attendance
  DROP CONSTRAINT IF EXISTS event_attendance_status_check;
ALTER TABLE event_attendance
  ADD CONSTRAINT event_attendance_status_check
  CHECK (status IN ('present','late','excused'));

CREATE OR REPLACE FUNCTION enforce_attendance_window() RETURNS TRIGGER
LANGUAGE plpgsql AS $$
DECLARE
  v_start    TIMESTAMPTZ;
  v_end      TIMESTAMPTZ;
  v_requires BOOLEAN;
  v_late_min INTEGER;
BEGIN
  -- Leader-proxy path: skip window + late-computation entirely. The leader
  -- has set status explicitly. We do NOT touch NEW.status or
  -- NEW.late_by_minutes here — the value the leader picked stands.
  IF NEW.posted_by_proxy IS NOT NULL THEN
    RETURN NEW;
  END IF;

  SELECT event_date, event_end_date, requires_attendance
    INTO v_start, v_end, v_requires
  FROM events
  WHERE id = NEW.event_id;

  IF v_start IS NULL THEN
    RAISE EXCEPTION 'Event not found';
  END IF;

  IF v_requires = FALSE THEN
    RAISE EXCEPTION 'Attendance is off for this event';
  END IF;

  IF NOW() < (v_start - INTERVAL '1 hour') THEN
    RAISE EXCEPTION 'Check-in opens 1 hour before the event';
  END IF;

  IF v_end IS NOT NULL THEN
    IF NOW() > v_end THEN
      RAISE EXCEPTION 'Check-in is closed for this event';
    END IF;
  ELSE
    IF NOW() > (v_start + INTERVAL '2 hours') THEN
      RAISE EXCEPTION 'Check-in is closed for this event';
    END IF;
  END IF;

  -- Compute server-side status for QR scans.
  IF NOW() <= v_start THEN
    NEW.status := 'present';
    NEW.late_by_minutes := 0;
  ELSE
    v_late_min := GREATEST(0, EXTRACT(EPOCH FROM (NOW() - v_start))::INTEGER / 60);
    NEW.status := 'late';
    NEW.late_by_minutes := v_late_min;
  END IF;

  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_enforce_attendance_window ON event_attendance;
CREATE TRIGGER trg_enforce_attendance_window
  BEFORE INSERT ON event_attendance
  FOR EACH ROW EXECUTE FUNCTION enforce_attendance_window();


DROP POLICY IF EXISTS "attendance_insert_proxy" ON event_attendance;
CREATE POLICY "attendance_insert_proxy" ON event_attendance
  FOR INSERT
  WITH CHECK (
    posted_by_proxy = auth.uid()
    AND EXISTS (
      SELECT 1 FROM users self
      WHERE self.id = auth.uid()
        AND self.role IN ('cell_leader','youth_president','pastor','admin')
        AND (
          self.role IN ('youth_president','pastor','admin')
          OR self.group_id = (
            SELECT group_id FROM users WHERE id = event_attendance.user_id
          )
        )
    )
  );


DROP POLICY IF EXISTS "attendance_delete_proxy" ON event_attendance;
CREATE POLICY "attendance_delete_proxy" ON event_attendance
  FOR DELETE USING (
    posted_by_proxy IS NOT NULL
    AND EXISTS (
      SELECT 1 FROM users self
      WHERE self.id = auth.uid()
        AND self.role IN ('cell_leader','youth_president','pastor','admin')
        AND (
          self.role IN ('youth_president','pastor','admin')
          OR self.group_id = (
            SELECT group_id FROM users WHERE id = event_attendance.user_id
          )
        )
    )
  );

DROP POLICY IF EXISTS "attendance_select_leader" ON event_attendance;
CREATE POLICY "attendance_select_leader" ON event_attendance
  FOR SELECT USING (
    EXISTS (
      SELECT 1 FROM users self
      WHERE self.id = auth.uid()
        AND self.role = 'cell_leader'
        AND self.group_id = (
          SELECT group_id FROM users WHERE id = event_attendance.user_id
        )
    )
  );

DO $$
BEGIN
  PERFORM 1 FROM information_schema.columns
    WHERE table_name = 'event_attendance' AND column_name = 'posted_by_proxy';
  IF NOT FOUND THEN RAISE EXCEPTION 'posted_by_proxy column missing'; END IF;
END $$;
