
CREATE TABLE IF NOT EXISTS event_attendance (
  event_id     UUID NOT NULL REFERENCES events(id) ON DELETE CASCADE,
  user_id      UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  attended_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  PRIMARY KEY (event_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_event_attendance_event ON event_attendance (event_id);

ALTER TABLE event_attendance ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "attendance_select" ON event_attendance;
DROP POLICY IF EXISTS "attendance_insert" ON event_attendance;
DROP POLICY IF EXISTS "attendance_delete" ON event_attendance;

CREATE POLICY "attendance_select" ON event_attendance
  FOR SELECT USING (
    auth.uid() = user_id
    OR auth.uid() = (SELECT created_by FROM events WHERE id = event_id)
    OR EXISTS (
      SELECT 1 FROM users
      WHERE id = auth.uid() AND role IN ('youth_president','pastor','admin')
    )
  );

CREATE POLICY "attendance_insert" ON event_attendance
  FOR INSERT WITH CHECK (auth.uid() = user_id);

CREATE POLICY "attendance_delete" ON event_attendance
  FOR DELETE USING (
    auth.uid() = user_id
    OR auth.uid() = (SELECT created_by FROM events WHERE id = event_id)
    OR EXISTS (
      SELECT 1 FROM users
      WHERE id = auth.uid() AND role IN ('youth_president','pastor','admin')
    )
  );

CREATE OR REPLACE FUNCTION enforce_attendance_window() RETURNS TRIGGER
LANGUAGE plpgsql AS $$
DECLARE
  v_event_date TIMESTAMPTZ;
BEGIN
  SELECT event_date INTO v_event_date FROM events WHERE id = NEW.event_id;
  IF v_event_date IS NULL THEN
    RAISE EXCEPTION 'Event not found';
  END IF;
  IF NOW() < (v_event_date - INTERVAL '1 hour') THEN
    RAISE EXCEPTION 'Check-in opens 1 hour before the event';
  END IF;
  IF NOW() > (v_event_date + INTERVAL '2 hours') THEN
    RAISE EXCEPTION 'Check-in closed 2 hours after the event';
  END IF;
  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_enforce_attendance_window ON event_attendance;
CREATE TRIGGER trg_enforce_attendance_window
  BEFORE INSERT ON event_attendance
  FOR EACH ROW EXECUTE FUNCTION enforce_attendance_window();
