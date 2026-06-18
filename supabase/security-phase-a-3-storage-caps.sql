
UPDATE storage.buckets
   SET file_size_limit    = 5242880,
       allowed_mime_types = ARRAY['image/jpeg','image/png','image/webp']
 WHERE id = 'posts';

INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
SELECT 'posts', 'posts', true, 5242880,
       ARRAY['image/jpeg','image/png','image/webp']
 WHERE NOT EXISTS (SELECT 1 FROM storage.buckets WHERE id = 'posts');

DROP POLICY IF EXISTS "posts_user_folder_and_quota" ON storage.objects;
CREATE POLICY "posts_user_folder_and_quota" ON storage.objects
  AS RESTRICTIVE
  FOR INSERT
  WITH CHECK (
    bucket_id <> 'posts'
    OR (
      auth.uid() IS NOT NULL
      AND (storage.foldername(name))[1] = auth.uid()::text
      AND (
        SELECT count(*) FROM storage.objects o
          WHERE o.bucket_id = 'posts'
            AND (storage.foldername(o.name))[1] = auth.uid()::text
      ) < 100
    )
  );

