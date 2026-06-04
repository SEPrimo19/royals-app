-- =============================================================================
-- GRACE — Bible Games v4: SECURITY DEFINER RPC for leaderboards
--
-- The base `game_attempts` RLS hides other users' rows from regular members
-- (own-rows only). That's correct for the raw table but breaks the leaderboard
-- view — a member would see only their own line. This RPC bypasses RLS for
-- AGGREGATED reads only, returning per-user totals without exposing the
-- individual attempt rows. Members can read their own group; leaders can
-- read any group.
--
-- Safe to re-run.
-- =============================================================================

CREATE OR REPLACE FUNCTION get_weekly_group_leaderboard(
  p_group_id UUID,
  p_limit    INTEGER DEFAULT 10
)
RETURNS TABLE (
  user_id       UUID,
  user_name     TEXT,
  group_id      UUID,
  week_points   INTEGER,
  week_attempts INTEGER
)
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  caller_group UUID;
  caller_role  TEXT;
BEGIN
  -- Pull the caller's group + role so we can decide who can see what.
  SELECT u.group_id, u.role
    INTO caller_group, caller_role
  FROM users u
  WHERE u.id = auth.uid();

  -- Anyone can read their OWN group. Cross-group reads require leader role.
  IF p_group_id IS DISTINCT FROM caller_group
     AND caller_role NOT IN ('cell_leader','youth_president','pastor','admin') THEN
    RAISE EXCEPTION 'Cross-group leaderboard reads require leader role';
  END IF;

  -- date_trunc('week', NOW()) = Monday 00:00 in Postgres. is_daily filters
  -- out Practice so the leaderboard only reflects Daily completions.
  RETURN QUERY
    SELECT
      a.user_id,
      u.name,
      u.group_id,
      SUM(a.points_earned)::INTEGER       AS week_points,
      COUNT(*)::INTEGER                   AS week_attempts
    FROM game_attempts a
    JOIN users u ON u.id = a.user_id
    WHERE u.group_id = p_group_id
      AND a.is_daily = TRUE
      AND a.played_at >= date_trunc('week', NOW())
    GROUP BY a.user_id, u.name, u.group_id
    ORDER BY SUM(a.points_earned) DESC
    LIMIT p_limit;
END;
$$;

-- Lock down PUBLIC; only authenticated users can call.
REVOKE ALL ON FUNCTION get_weekly_group_leaderboard(UUID, INTEGER) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION get_weekly_group_leaderboard(UUID, INTEGER) TO authenticated;
