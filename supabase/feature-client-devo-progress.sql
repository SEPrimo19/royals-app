-- =============================================================================
-- GRACE — Cross-device sync for client-generated daily devotionals
--
-- The Devo screen falls back to client-generated daily devotionals when
-- the server has none scheduled for today. Those have ids like
-- "daily-2026-06-04", which are NOT valid UUIDs, so they can't be written
-- to `user_devo_progress.devo_id` (UUID FK to devotionals(id)).
--
-- Result: completion for those days stays local-only — after a reinstall
-- (or sign-in on a second device), the Home ring drops back to 0% even
-- though the user already completed it.
--
-- Fix: a sidecar table that tracks completion by the client-generated
-- date key, with no FK. Repo writes to it on every client-daily mark,
-- syncMyDevoProgress() pulls from it to rebuild local Room rows.
--
-- Safe to re-run.
-- =============================================================================

CREATE TABLE IF NOT EXISTS user_client_devo_progress (
  user_id          UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  -- The client-generated key, e.g. "daily-2026-06-04". Free-form TEXT
  -- because the format is owned by the client; we don't validate here.
  client_devo_key  TEXT NOT NULL,
  completed_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  -- Encrypted journal payload (same AES-256-GCM ciphertext as the
  -- main user_devo_progress.journal_entry — base64 string).
  journal_entry    TEXT,
  PRIMARY KEY (user_id, client_devo_key)
);

CREATE INDEX IF NOT EXISTS idx_user_client_devo_progress_user
  ON user_client_devo_progress (user_id);

-- ---- RLS ----------------------------------------------------------------
-- Own-row only. Even leaders don't see this table — it's the same
-- privacy contract as the main user_devo_progress table.
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

-- Sanity check (run manually after the next mark-complete on a daily devo):
--   SELECT * FROM user_client_devo_progress WHERE user_id = auth.uid();
