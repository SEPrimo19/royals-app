
ALTER TABLE prayers ENABLE ROW LEVEL SECURITY;
ALTER TABLE posts   ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "prayers_update_owner" ON prayers;
DROP POLICY IF EXISTS "prayers_delete_owner" ON prayers;

CREATE POLICY "prayers_update_owner" ON prayers
  FOR UPDATE USING (
    auth.uid() = user_id
    OR EXISTS (
      SELECT 1 FROM users WHERE id = auth.uid()
        AND role IN ('youth_president','pastor','admin')
    )
  );

CREATE POLICY "prayers_delete_owner" ON prayers
  FOR DELETE USING (
    auth.uid() = user_id
    OR EXISTS (
      SELECT 1 FROM users WHERE id = auth.uid()
        AND role IN ('youth_president','pastor','admin')
    )
  );

DROP POLICY IF EXISTS "posts_update_owner" ON posts;
DROP POLICY IF EXISTS "posts_delete_owner" ON posts;

CREATE POLICY "posts_update_owner" ON posts
  FOR UPDATE USING (
    auth.uid() = user_id
    OR EXISTS (
      SELECT 1 FROM users WHERE id = auth.uid()
        AND role IN ('youth_president','pastor','admin')
    )
  );

CREATE POLICY "posts_delete_owner" ON posts
  FOR DELETE USING (
    auth.uid() = user_id
    OR EXISTS (
      SELECT 1 FROM users WHERE id = auth.uid()
        AND role IN ('youth_president','pastor','admin')
    )
  );
