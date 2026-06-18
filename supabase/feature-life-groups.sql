
ALTER TABLE groups         ENABLE ROW LEVEL SECURITY;
ALTER TABLE group_members  ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "groups_select" ON groups;
DROP POLICY IF EXISTS "groups_insert" ON groups;
DROP POLICY IF EXISTS "groups_update" ON groups;
DROP POLICY IF EXISTS "groups_delete" ON groups;

CREATE POLICY "groups_select" ON groups
  FOR SELECT USING (auth.role() = 'authenticated');

CREATE POLICY "groups_insert" ON groups
  FOR INSERT WITH CHECK (
    auth.uid() = leader_id
    AND EXISTS (
      SELECT 1 FROM users WHERE id = auth.uid()
        AND role IN ('cell_leader','youth_president','pastor','admin')
    )
  );

CREATE POLICY "groups_update" ON groups
  FOR UPDATE USING (
    auth.uid() = leader_id
    OR EXISTS (
      SELECT 1 FROM users WHERE id = auth.uid()
        AND role IN ('youth_president','pastor','admin')
    )
  );

CREATE POLICY "groups_delete" ON groups
  FOR DELETE USING (
    EXISTS (
      SELECT 1 FROM users WHERE id = auth.uid()
        AND role IN ('youth_president','pastor','admin')
    )
  );

DROP POLICY IF EXISTS "group_members_select" ON group_members;
DROP POLICY IF EXISTS "group_members_insert" ON group_members;
DROP POLICY IF EXISTS "group_members_delete" ON group_members;

CREATE POLICY "group_members_select" ON group_members
  FOR SELECT USING (auth.role() = 'authenticated');

CREATE POLICY "group_members_insert" ON group_members
  FOR INSERT WITH CHECK (
    auth.uid() = user_id
    OR auth.uid() = (SELECT leader_id FROM groups WHERE id = group_id)
    OR EXISTS (
      SELECT 1 FROM users WHERE id = auth.uid()
        AND role IN ('youth_president','pastor','admin')
    )
  );

CREATE POLICY "group_members_delete" ON group_members
  FOR DELETE USING (
    auth.uid() = user_id
    OR auth.uid() = (SELECT leader_id FROM groups WHERE id = group_id)
    OR EXISTS (
      SELECT 1 FROM users WHERE id = auth.uid()
        AND role IN ('youth_president','pastor','admin')
    )
  );

INSERT INTO group_members (user_id, group_id)
SELECT u.id, u.group_id
FROM users u
WHERE u.group_id IS NOT NULL
ON CONFLICT (user_id, group_id) DO NOTHING;
