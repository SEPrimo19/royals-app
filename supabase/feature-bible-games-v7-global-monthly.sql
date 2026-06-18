
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
  -- No role/group check — the global board is open to every authenticated
  -- user by design. We still authenticate (auth.uid() must be present)
  -- otherwise the RPC wouldn't be exposed to anon.
  IF auth.uid() IS NULL THEN
    RAISE EXCEPTION 'Authentication required';
  END IF;

  -- date_trunc('month', NOW()) snaps to the 1st of the current month at
  -- 00:00. So the board resets automatically when the calendar flips —
  -- no cron job needed. Includes both daily AND practice attempts.
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
      AND a.points_earned > 0  -- skip 0-point wrong/timed-out rows; they shouldn't help OR hurt ranking
    GROUP BY a.user_id, u.name, u.group_id, g.name
    ORDER BY SUM(a.points_earned) DESC, COUNT(*) ASC  -- ties broken by fewer attempts (efficiency)
    LIMIT p_limit;
END;
$$;

REVOKE ALL ON FUNCTION get_monthly_global_leaderboard(INTEGER) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION get_monthly_global_leaderboard(INTEGER) TO authenticated;

