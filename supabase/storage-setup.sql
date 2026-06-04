-- =============================================================================
-- GRACE — Storage setup (run once, safe to re-run)
--
-- Creates the two public buckets the app uploads to (`posts` for feed images,
-- `avatars` for profile pictures) and the RLS policies that let authenticated
-- users upload to their own UID folder while everyone can read.
--
-- Run in: Supabase Dashboard → SQL Editor → paste → Run.
-- (You can also create buckets in the Storage UI, but this is reproducible.)
-- =============================================================================

-- ---- Buckets (public so AsyncImage can fetch publicUrl() without auth) ------
INSERT INTO storage.buckets (id, name, public)
VALUES ('posts',   'posts',   true),
       ('avatars', 'avatars', true)
ON CONFLICT (id) DO NOTHING;

-- ---- Policies on storage.objects -------------------------------------------
-- Anyone can read files in posts/avatars (the buckets are public).
DROP POLICY IF EXISTS "grace_public_read" ON storage.objects;
CREATE POLICY "grace_public_read" ON storage.objects
  FOR SELECT USING (bucket_id IN ('posts', 'avatars'));

-- Any signed-in user can upload to posts/avatars.
DROP POLICY IF EXISTS "grace_authenticated_upload" ON storage.objects;
CREATE POLICY "grace_authenticated_upload" ON storage.objects
  FOR INSERT WITH CHECK (
    bucket_id IN ('posts', 'avatars')
    AND auth.role() = 'authenticated'
  );

-- A user can update/delete only their own files. Our upload path is
-- "{uid}/{uuid}.jpg", so storage.foldername(name)[1] === auth.uid().
DROP POLICY IF EXISTS "grace_update_own_files" ON storage.objects;
CREATE POLICY "grace_update_own_files" ON storage.objects
  FOR UPDATE USING (
    bucket_id IN ('posts', 'avatars')
    AND (storage.foldername(name))[1] = auth.uid()::text
  );

DROP POLICY IF EXISTS "grace_delete_own_files" ON storage.objects;
CREATE POLICY "grace_delete_own_files" ON storage.objects
  FOR DELETE USING (
    bucket_id IN ('posts', 'avatars')
    AND (storage.foldername(name))[1] = auth.uid()::text
  );
