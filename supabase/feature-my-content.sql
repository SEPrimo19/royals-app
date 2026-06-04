-- =============================================================================
-- GRACE — Feature: My Content (Profile screen with CRUD)
--
-- The schema enabled RLS on `prayers` and `posts` but the earliest scripts
-- only added SELECT and INSERT policies. UPDATE and DELETE were silently
-- blocked. This adds them — owner can edit/delete their own rows; senior
-- leaders can moderate.
--
-- Safe to re-run. DROP IF EXISTS pattern.
-- =============================================================================

ALTER TABLE prayers ENABLE ROW LEVEL SECURITY;
ALTER TABLE posts   ENABLE ROW LEVEL SECURITY;

-- ---- PRAYERS ---------------------------------------------------------------
DROP POLICY IF EXISTS "prayers_update_owner" ON prayers;
DROP POLICY IF EXISTS "prayers_delete_owner" ON prayers;

-- Owner can edit content + change status (e.g. mark answered). Senior
-- leaders can edit anyone's for moderation / flag clean-up.
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

-- ---- POSTS -----------------------------------------------------------------
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
