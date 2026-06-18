
DROP POLICY IF EXISTS "groups_insert" ON groups;

CREATE POLICY "groups_insert" ON groups
  FOR INSERT WITH CHECK (
    auth.uid() = leader_id
    AND EXISTS (
      SELECT 1 FROM users WHERE id = auth.uid()
        AND role IN ('cell_leader','council','youth_president','pastor','admin')
    )
  );
