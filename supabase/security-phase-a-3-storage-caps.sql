-- =============================================================================
-- GRACE — Security Phase A.3: Storage upload caps
--
-- Why this matters
-- ----------------
-- Life Feed photos upload to the `posts` bucket with NO server-side caps:
--   - No file-size limit (a malicious user could upload 50 MB images and
--     burn through your Storage quota / Egress cost)
--   - No MIME filter (anyone could upload .exe / .apk / random binaries
--     under a renamed .jpg)
--   - No per-user object count quota (one user could push thousands of
--     photos)
--
-- This migration adds all three guards:
--   1. Bucket-level file_size_limit = 5 MB
--   2. Bucket-level allowed_mime_types = images only
--   3. RLS restrictive policy: each user's uploads must land in their own
--      uid/ folder, and at most 100 objects per user in this bucket
--
-- The matching client-side guard in FeedRepositoryImpl ships in the app
-- update for friendly error UX.
--
-- Safe to re-run.
-- =============================================================================

-- ---- Bucket-level caps --------------------------------------------------
UPDATE storage.buckets
   SET file_size_limit    = 5242880,   -- 5 MB
       allowed_mime_types = ARRAY['image/jpeg','image/png','image/webp']
 WHERE id = 'posts';

-- If the bucket doesn't exist yet (fresh project), create it WITH the
-- limits already applied. Public read because Life Feed posts render
-- the image via `publicUrl()`.
INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
SELECT 'posts', 'posts', true, 5242880,
       ARRAY['image/jpeg','image/png','image/webp']
 WHERE NOT EXISTS (SELECT 1 FROM storage.buckets WHERE id = 'posts');

-- ---- RLS: per-user folder + 100-object quota ----------------------------
-- IMPORTANT: this is declared RESTRICTIVE so it AND's with whatever
-- existing PERMISSIVE policies allow the upload. PERMISSIVE policies are
-- OR'd by default, which would let any of them bypass quota; restrictive
-- forces "all permissive AND all restrictive must pass". For other
-- buckets we short-circuit to TRUE so this policy doesn't touch them.
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

-- Sanity check (run manually):
--   SELECT id, file_size_limit, allowed_mime_types FROM storage.buckets
--    WHERE id = 'posts';
-- Expected: file_size_limit = 5242880, allowed_mime_types =
--   {image/jpeg, image/png, image/webp}.
--
-- Upload test: from the app, picking a >5 MB photo should now produce a
-- friendly "Image is too large" toast instead of a generic upload failure.
