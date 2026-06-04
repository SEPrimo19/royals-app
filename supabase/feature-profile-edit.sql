-- =============================================================================
-- GRACE — Feature: Profile editor + Messenger link
--
-- Adds 3 columns to `users`:
--   - bio                 (short personal description, optional)
--   - messenger_url       (external contact URL — Messenger / FB / m.me)
--   - messenger_public    (privacy toggle: only show messenger to others if true)
--
-- No new RLS — the existing users_update policy already lets a user edit their
-- own row.
--
-- Safe to re-run (uses IF NOT EXISTS).
-- =============================================================================

ALTER TABLE users ADD COLUMN IF NOT EXISTS bio              TEXT;
ALTER TABLE users ADD COLUMN IF NOT EXISTS messenger_url    TEXT;
ALTER TABLE users ADD COLUMN IF NOT EXISTS messenger_public BOOLEAN DEFAULT FALSE;
