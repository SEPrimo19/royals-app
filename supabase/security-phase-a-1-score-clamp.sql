-- =============================================================================
-- GRACE — Security Phase A.1: server-side score clamping
--
-- Why this matters
-- ----------------
-- Today, `recordAttempt` accepts whatever `points_earned` the client posts.
-- A tampered APK or someone replaying network requests can write any number
-- they want — +9999 per attempt — and pollute the monthly global leaderboard
-- (or inflate their "This month" stat).
--
-- This trigger runs server-side BEFORE INSERT or UPDATE on `game_attempts`
-- and clamps `points_earned` to the maximum legal value for the row's
-- `mode`. It also forces `points_earned = 0` for any row where
-- `correct = false`. All legitimate inserts from the app pass through
-- unchanged; only out-of-range values get clamped.
--
-- Per-mode max:
--   trivia          → 30  (Hard correct)
--   fitb            → 25
--   who_am_i        → 40  (1-clue correct)
--   memory_match    → 30  (10/pair OR 30 perfect-clear bonus row)
--   verse_scramble  → 40  (30 base + 10 perfect bonus)
--   timeline_sort   → 60  (40 base + 20 perfect bonus)
--
-- A future mode that's added to the `mode` CHECK without updating this
-- trigger will get clamped to 0 (unknown mode → max = 0). Loud but safe:
-- you'll notice immediately because the new mode's points won't land. The
-- fix is one line in this function.
--
-- Safe to re-run.
-- =============================================================================

CREATE OR REPLACE FUNCTION clamp_game_attempt_points()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
  max_pts INTEGER;
BEGIN
  max_pts := CASE NEW.mode
    WHEN 'trivia'         THEN 30
    WHEN 'fitb'           THEN 25
    WHEN 'who_am_i'       THEN 40
    WHEN 'memory_match'   THEN 30
    WHEN 'verse_scramble' THEN 40
    WHEN 'timeline_sort'  THEN 60
    ELSE 0
  END;

  -- Negative is never valid.
  IF NEW.points_earned < 0 THEN
    NEW.points_earned := 0;
  END IF;

  -- Above per-mode max is rejected — clamped down silently.
  IF NEW.points_earned > max_pts THEN
    NEW.points_earned := max_pts;
  END IF;

  -- A wrong/timed-out attempt cannot award points, period.
  IF NEW.correct IS FALSE AND NEW.points_earned > 0 THEN
    NEW.points_earned := 0;
  END IF;

  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_clamp_game_attempt_points ON game_attempts;
CREATE TRIGGER trg_clamp_game_attempt_points
  BEFORE INSERT OR UPDATE ON game_attempts
  FOR EACH ROW
  EXECUTE FUNCTION clamp_game_attempt_points();

-- Sanity check (run manually):
--   INSERT INTO game_attempts (user_id, mode, correct, points_earned, is_daily)
--   VALUES (auth.uid(), 'trivia', true, 9999, false)
--   RETURNING points_earned;
-- Expected: points_earned = 30 (clamped from 9999 to the trivia max).
--
--   INSERT INTO game_attempts (user_id, mode, correct, points_earned, is_daily)
--   VALUES (auth.uid(), 'trivia', false, 30, false)
--   RETURNING points_earned;
-- Expected: points_earned = 0 (wrong attempts award zero).
