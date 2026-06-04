-- =============================================================================
-- GRACE — Weekly check-in: one row per user per ISO week, editable until Mon
--
-- Audit-item #5. Today every Submit appends a new `checkins` row, so the
-- leader's MemberDetail screen sees a pile of stacked submissions and
-- the user can't actually correct a check-in if they realized halfway
-- through the week that they'd written something wrong.
--
-- Fix: a `week_start DATE` column (auto-filled by trigger), UNIQUE on
-- (user_id, week_start). The client then UPSERTs targeting that
-- constraint — first submit of the week inserts, every subsequent
-- submit updates the same row. Resets automatically on Monday 00:00
-- (server time) when the truncated week boundary moves.
--
-- Postgres `date_trunc('week', NOW())` returns the Monday at 00:00 of the
-- current ISO 8601 week by default.
--
-- Safe to re-run.
-- =============================================================================

-- 1) Add the column (nullable for now so backfill works).
ALTER TABLE checkins
  ADD COLUMN IF NOT EXISTS week_start DATE;

-- 2) Backfill existing rows from their submitted_at.
UPDATE checkins
   SET week_start = date_trunc('week', submitted_at)::date
 WHERE week_start IS NULL;

-- 3) Auto-fill trigger for INSERT/UPDATE going forward.
CREATE OR REPLACE FUNCTION set_checkin_week_start()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
  NEW.week_start := date_trunc(
    'week', COALESCE(NEW.submitted_at, NOW())
  )::date;
  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_set_checkin_week_start ON checkins;
CREATE TRIGGER trg_set_checkin_week_start
  BEFORE INSERT OR UPDATE ON checkins
  FOR EACH ROW
  EXECUTE FUNCTION set_checkin_week_start();

-- 4) De-dupe historical rows (same user, same week): keep the most recent
--    submission as the canonical answer. Self-join trick is faster than a
--    window function on small tables and is easy to read.
DELETE FROM checkins a
  USING checkins b
 WHERE a.user_id    = b.user_id
   AND a.week_start = b.week_start
   AND a.submitted_at < b.submitted_at;

-- 5) Now safe to enforce NOT NULL + UNIQUE.
ALTER TABLE checkins
  ALTER COLUMN week_start SET NOT NULL;

ALTER TABLE checkins
  DROP CONSTRAINT IF EXISTS checkins_user_week_unique;
ALTER TABLE checkins
  ADD CONSTRAINT checkins_user_week_unique
  UNIQUE (user_id, week_start);

-- Sanity check (run manually):
--   SELECT user_id, week_start, count(*)
--     FROM checkins GROUP BY 1,2 HAVING count(*) > 1;
-- Expected: zero rows. If anything appears, the de-dupe missed something
-- — investigate before moving on.
--
--   INSERT INTO checkins (user_id, leader_id, answers)
--   VALUES (auth.uid(), NULL, '{"q1":"first","q2":"y","q3":"z"}'::jsonb);
--   -- Try inserting again with different answers in the SAME week:
--   INSERT INTO checkins (user_id, leader_id, answers)
--   VALUES (auth.uid(), NULL, '{"q1":"second","q2":"y","q3":"z"}'::jsonb)
--   ON CONFLICT (user_id, week_start)
--   DO UPDATE SET answers = EXCLUDED.answers, submitted_at = NOW();
-- Expected: still one row, with answers.q1 = 'second'.
