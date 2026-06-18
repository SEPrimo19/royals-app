
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

