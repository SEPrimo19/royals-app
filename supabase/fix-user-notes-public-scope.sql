
DROP POLICY IF EXISTS user_notes_select ON user_notes;

CREATE POLICY user_notes_select ON user_notes
  FOR SELECT USING (
    auth.uid() = user_id
    OR NOT is_hidden
    OR EXISTS (
      SELECT 1 FROM users
      WHERE id = auth.uid()
        AND role IN ('council','youth_president','pastor','admin')
    )
  );


CREATE OR REPLACE FUNCTION public.list_visible_notes()
RETURNS TABLE (
  user_id        UUID,
  user_name      TEXT,
  user_avatar    TEXT,
  content        TEXT,
  is_hidden      BOOLEAN,
  created_at     TIMESTAMPTZ,
  expires_at     TIMESTAMPTZ,
  heart_count    BIGINT,
  has_my_heart   BOOLEAN
)
LANGUAGE sql
SECURITY DEFINER
STABLE
SET search_path = public
AS $$
  WITH hc AS (
    SELECT note_user_id, count(*)::bigint AS c
      FROM user_note_hearts GROUP BY note_user_id
  )
  SELECT
    n.user_id,
    u.name,
    u.avatar_url,
    n.content,
    n.is_hidden,
    n.created_at,
    n.expires_at,
    COALESCE(hc.c, 0),
    EXISTS (
      SELECT 1 FROM user_note_hearts h
       WHERE h.note_user_id = n.user_id AND h.hearter_id = auth.uid()
    )
  FROM user_notes n
  JOIN users u ON u.id = n.user_id
  LEFT JOIN hc ON hc.note_user_id = n.user_id
  WHERE n.expires_at > NOW()
    AND (
      auth.uid() = n.user_id           -- your own
      OR NOT n.is_hidden               -- everyone else sees non-hidden
      OR EXISTS (                      -- senior sees hidden too
        SELECT 1 FROM users
        WHERE id = auth.uid()
          AND role IN ('council','youth_president','pastor','admin')
      )
    )
  ORDER BY n.created_at DESC;
$$;

GRANT EXECUTE ON FUNCTION public.list_visible_notes() TO authenticated;
