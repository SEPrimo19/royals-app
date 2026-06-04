-- =============================================================================
-- GRACE — Backfill: sync users.group_id from group_members
--
-- Bug: addMemberById in LifeGroupRepositoryImpl was inserting into the
-- group_members table but NOT mirroring to users.group_id. Result:
-- members appear in the Life Group roster but the Bible Games leaderboard
-- RPC (which joins via users.group_id) doesn't see them, and the member's
-- client-side prefs.groupId stays NULL.
--
-- App-side fix is in LifeGroupRepositoryImpl. This SQL fixes users who
-- were added BEFORE that change shipped.
--
-- Rule: if a user has at least one group_members row AND users.group_id
-- is NULL, set users.group_id to their earliest-joined group. If
-- users.group_id is already set (e.g. they joined via createLifeGroup or
-- ProfileSetup), leave it alone.
--
-- Uses DISTINCT ON instead of MIN(group_id) because Postgres has no
-- aggregate MIN for the UUID type.
--
-- Safe to re-run.
-- =============================================================================

UPDATE users u
SET group_id = first_group.group_id
FROM (
  SELECT DISTINCT ON (user_id) user_id, group_id
  FROM group_members
  ORDER BY user_id, joined_at
) first_group
WHERE u.id = first_group.user_id
  AND u.group_id IS NULL;

-- Sanity-check the result. Should return 0 rows after this runs.
SELECT u.id, u.name, u.email
FROM users u
JOIN group_members gm ON gm.user_id = u.id
WHERE u.group_id IS NULL;
