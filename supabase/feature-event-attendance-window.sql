
ALTER TABLE events
  ADD COLUMN IF NOT EXISTS event_end_date TIMESTAMPTZ;
ALTER TABLE events
  ADD COLUMN IF NOT EXISTS requires_attendance BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE event_attendance
  ADD COLUMN IF NOT EXISTS status TEXT NOT NULL DEFAULT 'present'
    CHECK (status IN ('present','late'));
ALTER TABLE event_attendance
  ADD COLUMN IF NOT EXISTS late_by_minutes INTEGER NOT NULL DEFAULT 0;

CREATE OR REPLACE FUNCTION enforce_attendance_window() RETURNS TRIGGER
LANGUAGE plpgsql AS $$
DECLARE
  v_start    TIMESTAMPTZ;
  v_end      TIMESTAMPTZ;
  v_requires BOOLEAN;
  v_late_min INTEGER;
BEGIN
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

  -- Lower bound: open 1 hour before start (unchanged from v1).
  IF NOW() < (v_start - INTERVAL '1 hour') THEN
    RAISE EXCEPTION 'Check-in opens 1 hour before the event';
  END IF;

  -- Upper bound: explicit end if set, otherwise legacy +2h fallback.
  IF v_end IS NOT NULL THEN
    IF NOW() > v_end THEN
      RAISE EXCEPTION 'Check-in is closed for this event';
    END IF;
  ELSE
    IF NOW() > (v_start + INTERVAL '2 hours') THEN
      RAISE EXCEPTION 'Check-in is closed for this event';
    END IF;
  END IF;

  -- Compute server-side status. Anything past start is "late".
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
