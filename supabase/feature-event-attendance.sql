-- =============================================================================
-- GRACE — Feature: Event QR Attendance
--
-- Members scan a QR code at the event venue (deep link grace://event-checkin/
-- {event_id}) and an `event_attendance` row is created automatically.
--
-- The time-window trigger blocks inserts outside `event_date - 1h` to
-- `event_date + 2h` — prevents someone screenshotting the QR and "attending"
-- from home later. Composite PK prevents duplicate check-ins.
--
-- Visibility:
--   - Members can read their own attendance rows (have I attended?)
--   - Event creator + senior leaders can see the full attendee list
--   - INSERT must be by the user themselves (auth.uid() = user_id)
--
-- Safe to re-run.
-- =============================================================================

CREATE TABLE IF NOT EXISTS event_attendance (
  event_id     UUID NOT NULL REFERENCES events(id) ON DELETE CASCADE,
  user_id      UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  attended_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  PRIMARY KEY (event_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_event_attendance_event ON event_attendance (event_id);

ALTER TABLE event_attendance ENABLE ROW LEVEL SECURITY;

-- ---- RLS POLICIES ----------------------------------------------------------
DROP POLICY IF EXISTS "attendance_select" ON event_attendance;
DROP POLICY IF EXISTS "attendance_insert" ON event_attendance;
DROP POLICY IF EXISTS "attendance_delete" ON event_attendance;

-- A user can always see their own rows; the event creator and senior leaders
-- can see the full roster.
CREATE POLICY "attendance_select" ON event_attendance
  FOR SELECT USING (
    auth.uid() = user_id
    OR auth.uid() = (SELECT created_by FROM events WHERE id = event_id)
    OR EXISTS (
      SELECT 1 FROM users
      WHERE id = auth.uid() AND role IN ('youth_president','pastor','admin')
    )
  );

-- Only the user themselves can mark themselves attended. The time-window
-- trigger below adds the temporal guard.
CREATE POLICY "attendance_insert" ON event_attendance
  FOR INSERT WITH CHECK (auth.uid() = user_id);

-- The user can remove their own check-in (mis-tap); creator/senior leaders
-- can also remove (data correction).
CREATE POLICY "attendance_delete" ON event_attendance
  FOR DELETE USING (
    auth.uid() = user_id
    OR auth.uid() = (SELECT created_by FROM events WHERE id = event_id)
    OR EXISTS (
      SELECT 1 FROM users
      WHERE id = auth.uid() AND role IN ('youth_president','pastor','admin')
    )
  );

-- ---- TIME-WINDOW TRIGGER ---------------------------------------------------
-- Allows check-in only while NOW() is within 1h before to 2h after event_date.
-- This is the main defense against "screenshot the QR, check in from home".
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
