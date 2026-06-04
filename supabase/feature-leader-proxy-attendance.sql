-- =============================================================================
-- Royals: The Kingdom Builders — Leader Proxy Attendance (Phase P.2)
--
-- Lets a cell leader mark attendance on behalf of cell members who can't
-- scan the QR themselves (no smartphone, lost phone, etc.). Builds on the
-- Phase P.1 foundation (is_proxy_only column) BUT works for ANY cell
-- member — a leader can also mark a regular app-user member if they
-- arrived late without their phone.
--
-- Schema additions:
--   event_attendance.posted_by_proxy  UUID — leader who recorded this row
--                                     (NULL = member-initiated QR scan)
--   event_attendance.status           CHECK extended to allow 'excused'
--
-- Behavioral changes:
--   - Time-window trigger SKIPS the window check when posted_by_proxy
--     IS NOT NULL. Leaders are trusted to record attendance accurately;
--     they often log everyone AFTER the meeting wraps up.
--   - New INSERT policy lets a leader insert proxy rows for any member
--     in their cell (cell_leader) or any member at all (senior leaders).
--
-- Safe to re-run.
-- =============================================================================

-- ---- COLUMNS ----------------------------------------------------------------
ALTER TABLE event_attendance
  ADD COLUMN IF NOT EXISTS posted_by_proxy UUID REFERENCES users(id);

-- Extend the status CHECK to include 'excused'. The existing constraint
-- only allows 'present' | 'late' — drop + recreate to widen it.
ALTER TABLE event_attendance
  DROP CONSTRAINT IF EXISTS event_attendance_status_check;
ALTER TABLE event_attendance
  ADD CONSTRAINT event_attendance_status_check
  CHECK (status IN ('present','late','excused'));

-- ---- TRIGGER REPLACEMENT ----------------------------------------------------
-- Same logic as feature-event-attendance-window.sql, but with one extra
-- early-out: if posted_by_proxy IS NOT NULL, this is a leader recording
-- attendance on behalf of a member. The leader picks the status explicitly
-- ('present'|'late'|'excused') so the trigger respects their choice and
-- skips the window enforcement entirely.
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

-- ---- RLS POLICY: leader proxy INSERT ----------------------------------------
-- The original "attendance_insert" policy enforced `auth.uid() = user_id` —
-- only the member themselves could mark themselves attended. We add a SECOND
-- INSERT policy specifically for leader-proxy inserts. Postgres treats
-- multiple permissive INSERT policies as OR'd, so the original member-self
-- path still works untouched.

DROP POLICY IF EXISTS "attendance_insert_proxy" ON event_attendance;
CREATE POLICY "attendance_insert_proxy" ON event_attendance
  FOR INSERT
  WITH CHECK (
    -- Caller must be the leader stamped on the row (no impersonation —
    -- a leader can't post a proxy row claiming someone else did the work).
    posted_by_proxy = auth.uid()
    AND EXISTS (
      SELECT 1 FROM users self
      WHERE self.id = auth.uid()
        AND self.role IN ('cell_leader','youth_president','pastor','admin')
        AND (
          -- Senior leaders: mark attendance for anyone
          self.role IN ('youth_president','pastor','admin')
          -- Cell leaders: only their own cell members. Check via the
          -- target member's group_id matching the leader's.
          OR self.group_id = (
            SELECT group_id FROM users WHERE id = event_attendance.user_id
          )
        )
    )
  );

-- ---- RLS POLICY: leader proxy DELETE / UPDATE -------------------------------
-- Leaders need to be able to UNDO a proxy entry (typo'd the wrong member,
-- marked someone present who actually didn't show). Original delete policy
-- already covers creator + senior leaders; this adds cell-leader undo for
-- their own cell's proxy rows.

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

-- ---- RLS POLICY: leader SELECT for their cell -------------------------------
-- Cell leaders need to SEE their members' attendance to render the roster
-- screen (P.2 UI). Original SELECT policy covers creator + senior leaders;
-- this widens to cell leaders for their cell.
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

-- ---- Verification (silent if all good) --------------------------------------
DO $$
BEGIN
  PERFORM 1 FROM information_schema.columns
    WHERE table_name = 'event_attendance' AND column_name = 'posted_by_proxy';
  IF NOT FOUND THEN RAISE EXCEPTION 'posted_by_proxy column missing'; END IF;
END $$;
