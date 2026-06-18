
INSERT INTO storage.buckets (id, name, public)
VALUES ('posts',   'posts',   true),
       ('avatars', 'avatars', true)
ON CONFLICT (id) DO NOTHING;

DROP POLICY IF EXISTS "grace_public_read" ON storage.objects;
CREATE POLICY "grace_public_read" ON storage.objects
  FOR SELECT USING (bucket_id IN ('posts', 'avatars'));

DROP POLICY IF EXISTS "grace_authenticated_upload" ON storage.objects;
CREATE POLICY "grace_authenticated_upload" ON storage.objects
  FOR INSERT WITH CHECK (
    bucket_id IN ('posts', 'avatars')
    AND auth.role() = 'authenticated'
  );

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
