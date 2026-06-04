-- =============================================================================
-- GRACE — Feature: Life Groups (cell groups / small-group ministry)
--
-- The `groups` and `group_members` tables already exist (created by
-- schema.sql) but had no RLS policies. This enables RLS, adds policies, and
-- (optionally) backfills group_members from existing users.group_id values
-- so the Life Group screen can see members ProfileSetup added under the old
-- model.
--
-- Safe to re-run. Run in: Supabase Dashboard → SQL Editor → paste → Run.
-- =============================================================================

ALTER TABLE groups         ENABLE ROW LEVEL SECURITY;
ALTER TABLE group_members  ENABLE ROW LEVEL SECURITY;

-- ---- GROUPS ----------------------------------------------------------------
DROP POLICY IF EXISTS "groups_select" ON groups;
DROP POLICY IF EXISTS "groups_insert" ON groups;
DROP POLICY IF EXISTS "groups_update" ON groups;
DROP POLICY IF EXISTS "groups_delete" ON groups;

-- Anyone signed in can see groups (needed for ProfileSetup and browsing).
CREATE POLICY "groups_select" ON groups
  FOR SELECT USING (auth.role() = 'authenticated');

-- Only leaders create groups, and they must list themselves as leader_id
-- (prevents drive-by creation of groups assigned to others).
CREATE POLICY "groups_insert" ON groups
  FOR INSERT WITH CHECK (
    auth.uid() = leader_id
    AND EXISTS (
      SELECT 1 FROM users WHERE id = auth.uid()
        AND role IN ('cell_leader','youth_president','pastor','admin')
    )
  );

-- The group's leader OR senior leaders can rename/update.
CREATE POLICY "groups_update" ON groups
  FOR UPDATE USING (
    auth.uid() = leader_id
    OR EXISTS (
      SELECT 1 FROM users WHERE id = auth.uid()
        AND role IN ('youth_president','pastor','admin')
    )
  );

-- Only senior leaders can delete a group (cell_leader can rename but not drop
-- their own — destructive action, escalate).
CREATE POLICY "groups_delete" ON groups
  FOR DELETE USING (
    EXISTS (
      SELECT 1 FROM users WHERE id = auth.uid()
        AND role IN ('youth_president','pastor','admin')
    )
  );

-- ---- GROUP MEMBERS ---------------------------------------------------------
DROP POLICY IF EXISTS "group_members_select" ON group_members;
DROP POLICY IF EXISTS "group_members_insert" ON group_members;
DROP POLICY IF EXISTS "group_members_delete" ON group_members;

-- Membership rosters are visible to all signed-in users.
CREATE POLICY "group_members_select" ON group_members
  FOR SELECT USING (auth.role() = 'authenticated');

-- Insert allowed if:
--   1. The user is adding themselves (used by createLifeGroup self-insert), OR
--   2. The caller is the group's leader (adding members), OR
--   3. The caller is a senior leader.
CREATE POLICY "group_members_insert" ON group_members
  FOR INSERT WITH CHECK (
    auth.uid() = user_id
    OR auth.uid() = (SELECT leader_id FROM groups WHERE id = group_id)
    OR EXISTS (
      SELECT 1 FROM users WHERE id = auth.uid()
        AND role IN ('youth_president','pastor','admin')
    )
  );

-- Delete allowed if:
--   1. The user is removing themselves (leave-group), OR
--   2. The caller is the group's leader (kicking members), OR
--   3. The caller is a senior leader.
CREATE POLICY "group_members_delete" ON group_members
  FOR DELETE USING (
    auth.uid() = user_id
    OR auth.uid() = (SELECT leader_id FROM groups WHERE id = group_id)
    OR EXISTS (
      SELECT 1 FROM users WHERE id = auth.uid()
        AND role IN ('youth_president','pastor','admin')
    )
  );

-- ---- BACKFILL (one-time, idempotent) --------------------------------------
-- Bring everyone with a legacy users.group_id into the group_members table so
-- they appear in their Life Group's member list.
INSERT INTO group_members (user_id, group_id)
SELECT u.id, u.group_id
FROM users u
WHERE u.group_id IS NOT NULL
ON CONFLICT (user_id, group_id) DO NOTHING;
