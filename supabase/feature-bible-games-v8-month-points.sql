-- =============================================================================
-- GRACE — Bible Games v8: per-user "points this month" helper
--
-- The Bible Games hub used to show lifetime cumulative points in the big
-- TOTAL POINTS stat. We're switching that to a monthly sum so it aligns
-- with the global monthly leaderboard (both reset on the 1st of the month).
--
-- We compute server-side via this RPC instead of summing on the client so
-- the month boundary stays IDENTICAL to the leaderboard's `date_trunc
-- ('month', NOW())` — no timezone skew. Counts BOTH Daily + Practice
-- points, same as the leaderboard.
--
-- Safe to re-run.
-- =============================================================================

CREATE OR REPLACE FUNCTION get_my_month_points()
RETURNS INTEGER
LANGUAGE plpgsql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  result INTEGER;
BEGIN
  IF auth.uid() IS NULL THEN
    RETURN 0;
  END IF;
  SELECT COALESCE(SUM(points_earned), 0)::INTEGER
    INTO result
    FROM game_attempts
   WHERE user_id = auth.uid()
     AND played_at >= date_trunc('month', NOW());
  RETURN result;
END;
$$;

REVOKE ALL ON FUNCTION get_my_month_points() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION get_my_month_points() TO authenticated;

-- Sanity check (run manually — will return 0 from the SQL Editor since
-- auth.uid() is NULL when running as postgres; the app gets the real sum):
--   SELECT get_my_month_points();
