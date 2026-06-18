
CREATE OR REPLACE FUNCTION get_team_leaderboard(
  p_limit INTEGER DEFAULT 25
)
RETURNS TABLE (
  group_id     UUID,
  group_name   TEXT,
  member_count BIGINT,
  month_points BIGINT
)
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
  IF auth.uid() IS NULL THEN
    RAISE EXCEPTION 'Authentication required';
  END IF;

  RETURN QUERY
    SELECT
      g.id   AS group_id,
      g.name AS group_name,
      COUNT(DISTINCT a.user_id) AS member_count,
      SUM(a.points_earned)::BIGINT AS month_points
    FROM game_attempts a
    JOIN users  u ON u.id = a.user_id
    JOIN groups g ON g.id = u.group_id
    WHERE a.played_at >= date_trunc('month', NOW())
    GROUP BY g.id, g.name
    -- Highest total first; ties broken by the smaller team (more efficient).
    ORDER BY SUM(a.points_earned) DESC, COUNT(DISTINCT a.user_id) ASC
    LIMIT p_limit;
END;
$$;

REVOKE ALL ON FUNCTION get_team_leaderboard(INTEGER) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION get_team_leaderboard(INTEGER) TO authenticated;
GRANT EXECUTE ON FUNCTION get_team_leaderboard(INTEGER) TO postgres, service_role;
