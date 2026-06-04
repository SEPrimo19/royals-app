-- =============================================================================
-- GRACE — Feature: Event QR time-limit + attendance toggle
--
-- Builds on `feature-event-attendance.sql`. Adds:
--
--   1. events.event_end_date          (TIMESTAMPTZ, NULL = use legacy +2h)
--   2. events.requires_attendance     (BOOLEAN, default TRUE)
--   3. event_attendance.status        ('present' or 'late' — computed in trigger)
--   4. event_attendance.late_by_minutes (INTEGER, computed in trigger)
--
-- Behavior (per user spec):
--   - Scan BEFORE event_date        → status='present', late_by_minutes=0
--   - Scan BETWEEN start and end    → status='late',    late_by_minutes=N
--   - Scan AFTER event_end_date     → rejected ("Check-in is closed")
--   - requires_attendance=false     → rejected ("Attendance is off for this event")
--   - "Absent" is NOT stored — it's the creator-roster derivation
--     (RSVP='going' minus actual check-ins), computed on read after the event
--     ends. Storing absent rows would flood the table with church-wide entries.
--
-- Safe to re-run.
-- =============================================================================

-- ---- COLUMNS: events --------------------------------------------------------
ALTER TABLE events
  ADD COLUMN IF NOT EXISTS event_end_date TIMESTAMPTZ;
ALTER TABLE events
  ADD COLUMN IF NOT EXISTS requires_attendance BOOLEAN NOT NULL DEFAULT TRUE;

-- ---- COLUMNS: event_attendance ---------------------------------------------
ALTER TABLE event_attendance
  ADD COLUMN IF NOT EXISTS status TEXT NOT NULL DEFAULT 'present'
    CHECK (status IN ('present','late'));
ALTER TABLE event_attendance
  ADD COLUMN IF NOT EXISTS late_by_minutes INTEGER NOT NULL DEFAULT 0;

-- ---- TRIGGER REPLACEMENT ---------------------------------------------------
-- Computes status + late_by_minutes server-side. The client only reads them.
-- This is the single source of truth — no client-trust for "was I late?".
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

-- Re-bind the trigger to pick up the new function body.
DROP TRIGGER IF EXISTS trg_enforce_attendance_window ON event_attendance;
CREATE TRIGGER trg_enforce_attendance_window
  BEFORE INSERT ON event_attendance
  FOR EACH ROW EXECUTE FUNCTION enforce_attendance_window();
