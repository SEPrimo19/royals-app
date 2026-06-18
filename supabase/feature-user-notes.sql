
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";


CREATE TABLE IF NOT EXISTS user_notes (
  user_id     UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
  content     TEXT NOT NULL CHECK (length(trim(content)) BETWEEN 1 AND 200),
  is_hidden   BOOLEAN NOT NULL DEFAULT FALSE,
  hidden_by   UUID REFERENCES users(id),
  hidden_at   TIMESTAMPTZ,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  expires_at  TIMESTAMPTZ NOT NULL DEFAULT (NOW() + INTERVAL '24 hours')
);

CREATE TABLE IF NOT EXISTS user_note_hearts (
  note_user_id  UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  hearter_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  PRIMARY KEY (note_user_id, hearter_id)
);

CREATE INDEX IF NOT EXISTS idx_user_notes_expires ON user_notes (expires_at);
CREATE INDEX IF NOT EXISTS idx_user_note_hearts_note ON user_note_hearts (note_user_id);


CREATE OR REPLACE FUNCTION public.clear_note_hearts_on_replace()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
  IF NEW.created_at IS DISTINCT FROM OLD.created_at THEN
    DELETE FROM user_note_hearts WHERE note_user_id = NEW.user_id;
  END IF;
  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_clear_note_hearts ON user_notes;
CREATE TRIGGER trg_clear_note_hearts
  AFTER UPDATE ON user_notes
  FOR EACH ROW
  EXECUTE FUNCTION public.clear_note_hearts_on_replace();

CREATE OR REPLACE FUNCTION public.clear_note_hearts_on_delete()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
  DELETE FROM user_note_hearts WHERE note_user_id = OLD.user_id;
  RETURN OLD;
END;
$$;

DROP TRIGGER IF EXISTS trg_clear_note_hearts_on_delete ON user_notes;
CREATE TRIGGER trg_clear_note_hearts_on_delete
  AFTER DELETE ON user_notes
  FOR EACH ROW
  EXECUTE FUNCTION public.clear_note_hearts_on_delete();


ALTER TABLE user_notes ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_note_hearts ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS user_notes_select ON user_notes;
DROP POLICY IF EXISTS user_notes_upsert_self ON user_notes;
DROP POLICY IF EXISTS user_notes_delete_self_or_leader ON user_notes;
DROP POLICY IF EXISTS user_notes_hide_leader ON user_notes;

CREATE POLICY user_notes_select ON user_notes
  FOR SELECT USING (
    auth.uid() = user_id
    OR (
      NOT is_hidden
      AND user_id IN (
        SELECT id FROM users
        WHERE group_id IS NOT NULL
          AND group_id = (SELECT group_id FROM users WHERE id = auth.uid())
      )
    )
    OR EXISTS (
      SELECT 1 FROM users
      WHERE id = auth.uid()
        AND role IN ('council','youth_president','pastor','admin')
    )
  );

CREATE POLICY user_notes_upsert_self ON user_notes
  FOR INSERT WITH CHECK (auth.uid() = user_id);

CREATE POLICY user_notes_hide_leader ON user_notes
  FOR UPDATE USING (
    auth.uid() = user_id
    OR EXISTS (
      SELECT 1 FROM users
      WHERE id = auth.uid()
        AND role IN ('cell_leader','council','youth_president','pastor','admin')
    )
  ) WITH CHECK (
    auth.uid() = user_id
    OR EXISTS (
      SELECT 1 FROM users
      WHERE id = auth.uid()
        AND role IN ('cell_leader','council','youth_president','pastor','admin')
    )
  );

CREATE POLICY user_notes_delete_self_or_leader ON user_notes
  FOR DELETE USING (
    auth.uid() = user_id
    OR EXISTS (
      SELECT 1 FROM users
      WHERE id = auth.uid()
        AND role IN ('cell_leader','council','youth_president','pastor','admin')
    )
  );


DROP POLICY IF EXISTS user_note_hearts_select ON user_note_hearts;
DROP POLICY IF EXISTS user_note_hearts_insert_self ON user_note_hearts;
DROP POLICY IF EXISTS user_note_hearts_delete_self ON user_note_hearts;

CREATE POLICY user_note_hearts_select ON user_note_hearts
  FOR SELECT USING (auth.role() = 'authenticated');

CREATE POLICY user_note_hearts_insert_self ON user_note_hearts
  FOR INSERT WITH CHECK (auth.uid() = hearter_id);

CREATE POLICY user_note_hearts_delete_self ON user_note_hearts
  FOR DELETE USING (auth.uid() = hearter_id);


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
    SELECT note_user_id, count(*)::bigint AS c FROM user_note_hearts GROUP BY note_user_id
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
    -- Re-apply the same visibility rules as the table policy so the RPC
    -- (SECURITY DEFINER) doesn't bypass them.
    AND (
      auth.uid() = n.user_id
      OR (
        NOT n.is_hidden
        AND n.user_id IN (
          SELECT id FROM users
          WHERE group_id IS NOT NULL
            AND group_id = (SELECT group_id FROM users WHERE id = auth.uid())
        )
      )
      OR EXISTS (
        SELECT 1 FROM users
        WHERE id = auth.uid()
          AND role IN ('council','youth_president','pastor','admin')
      )
    )
  ORDER BY n.created_at DESC;
$$;

GRANT EXECUTE ON FUNCTION public.list_visible_notes() TO authenticated;


CREATE OR REPLACE FUNCTION public.sweep_expired_notes()
RETURNS INT
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE v_n INT;
BEGIN
  WITH d AS (
    DELETE FROM user_notes WHERE expires_at < NOW() - INTERVAL '7 days'
    RETURNING 1
  )
  SELECT count(*) INTO v_n FROM d;
  RETURN v_n;
END;
$$;
GRANT EXECUTE ON FUNCTION public.sweep_expired_notes() TO postgres, service_role;
