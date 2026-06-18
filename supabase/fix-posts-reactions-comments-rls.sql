
DROP POLICY IF EXISTS "posts_select"        ON posts;
DROP POLICY IF EXISTS "posts_insert_own"    ON posts;
DROP POLICY IF EXISTS "posts_update_own"    ON posts;
DROP POLICY IF EXISTS "posts_update_leader" ON posts;
DROP POLICY IF EXISTS "posts_delete_own"    ON posts;

CREATE POLICY "posts_select" ON posts
  FOR SELECT USING (auth.role() = 'authenticated');

CREATE POLICY "posts_insert_own" ON posts
  FOR INSERT WITH CHECK (auth.uid() = user_id);

CREATE POLICY "posts_update_own" ON posts
  FOR UPDATE USING (auth.uid() = user_id);

CREATE POLICY "posts_update_leader" ON posts
  FOR UPDATE USING (
    EXISTS (
      SELECT 1 FROM users
      WHERE id = auth.uid()
        AND role IN ('cell_leader','youth_president','pastor','admin')
    )
  );

CREATE POLICY "posts_delete_own" ON posts
  FOR DELETE USING (auth.uid() = user_id);

DROP POLICY IF EXISTS "reactions_select"     ON reactions;
DROP POLICY IF EXISTS "reactions_insert_own" ON reactions;
DROP POLICY IF EXISTS "reactions_update_own" ON reactions;
DROP POLICY IF EXISTS "reactions_delete_own" ON reactions;

CREATE POLICY "reactions_select" ON reactions
  FOR SELECT USING (auth.role() = 'authenticated');

CREATE POLICY "reactions_insert_own" ON reactions
  FOR INSERT WITH CHECK (auth.uid() = user_id);

CREATE POLICY "reactions_update_own" ON reactions
  FOR UPDATE USING (auth.uid() = user_id);

CREATE POLICY "reactions_delete_own" ON reactions
  FOR DELETE USING (auth.uid() = user_id);

DROP POLICY IF EXISTS "comments_select"     ON comments;
DROP POLICY IF EXISTS "comments_insert_own" ON comments;
DROP POLICY IF EXISTS "comments_delete_own" ON comments;

CREATE POLICY "comments_select" ON comments
  FOR SELECT USING (auth.role() = 'authenticated');

CREATE POLICY "comments_insert_own" ON comments
  FOR INSERT WITH CHECK (auth.uid() = user_id);

CREATE POLICY "comments_delete_own" ON comments
  FOR DELETE USING (auth.uid() = user_id);

DROP POLICY IF EXISTS "mood_insert_own" ON mood_checkins;

CREATE POLICY "mood_insert_own" ON mood_checkins
  FOR INSERT WITH CHECK (auth.uid() = user_id);
