
CREATE TABLE IF NOT EXISTS user_client_devo_progress (
  user_id          UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  client_devo_key  TEXT NOT NULL,
  completed_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  journal_entry    TEXT,
  PRIMARY KEY (user_id, client_devo_key)
);

CREATE INDEX IF NOT EXISTS idx_user_client_devo_progress_user
  ON user_client_devo_progress (user_id);

ALTER TABLE user_client_devo_progress ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "ucdp_select_own"  ON user_client_devo_progress;
DROP POLICY IF EXISTS "ucdp_insert_own"  ON user_client_devo_progress;
DROP POLICY IF EXISTS "ucdp_update_own"  ON user_client_devo_progress;
DROP POLICY IF EXISTS "ucdp_delete_own"  ON user_client_devo_progress;

CREATE POLICY "ucdp_select_own" ON user_client_devo_progress
  FOR SELECT USING (auth.uid() = user_id);

CREATE POLICY "ucdp_insert_own" ON user_client_devo_progress
  FOR INSERT WITH CHECK (auth.uid() = user_id);

CREATE POLICY "ucdp_update_own" ON user_client_devo_progress
  FOR UPDATE USING (auth.uid() = user_id);

CREATE POLICY "ucdp_delete_own" ON user_client_devo_progress
  FOR DELETE USING (auth.uid() = user_id);

