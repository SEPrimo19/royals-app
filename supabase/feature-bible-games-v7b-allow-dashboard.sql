-- =============================================================================
-- GRACE — Bible Games v7b: drop the redundant auth.uid() check inside the
-- monthly global RPC.
--
-- The original v7 raised "Authentication required" when auth.uid() was NULL.
-- That blocks the Supabase SQL Editor (which runs as the postgres role with
-- no auth context) but adds no security — `REVOKE ALL FROM PUBLIC` and
-- `GRANT EXECUTE TO authenticated` already prevent anonymous external
-- callers from invoking the RPC.
--
-- Safe to re-run.
-- =============================================================================

CREATE OR REPLACE FUNCTION get_monthly_global_leaderboard(
  p_limit INTEGER DEFAULT 25
)
RETURNS TABLE (
  user_id        UUID,
  user_name      TEXT,
  group_id       UUID,
  group_name     TEXT,
  month_points   INTEGER,
  month_attempts INTEGER
)
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
  -- No explicit auth.uid() check — the GRANT/REVOKE on this function already
  -- limits invocation to the `authenticated` role from PostgREST. Running
  -- this from the SQL Editor as `postgres` is intentional (admin sanity).
  RETURN QUERY
    SELECT
      a.user_id,
      u.name           AS user_name,
      u.group_id,
      g.name           AS group_name,
      SUM(a.points_earned)::INTEGER AS month_points,
      COUNT(*)::INTEGER             AS month_attempts
    FROM game_attempts a
    JOIN users  u ON u.id = a.user_id
    LEFT JOIN groups g ON g.id = u.group_id
    WHERE a.played_at >= date_trunc('month', NOW())
      AND a.points_earned > 0
    GROUP BY a.user_id, u.name, u.group_id, g.name
    ORDER BY SUM(a.points_earned) DESC, COUNT(*) ASC
    LIMIT p_limit;
END;
$$;

REVOKE ALL ON FUNCTION get_monthly_global_leaderboard(INTEGER) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION get_monthly_global_leaderboard(INTEGER) TO authenticated;
