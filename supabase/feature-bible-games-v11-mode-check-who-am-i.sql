-- =============================================================================
-- GRACE — Bible Games v11: widen game_attempts.mode CHECK to include who_am_i
--
-- The original v1 schema defined `mode TEXT NOT NULL CHECK (mode IN
-- ('trivia','fitb'))`. When v9 added "Who Am I?" the client started
-- inserting mode='who_am_i', which the CHECK rejected — the insert
-- threw, the repo's try/catch swallowed the error, and points silently
-- vanished. Only Trivia + FITB attempts were landing in game_attempts.
--
-- Postgres can't ALTER a CHECK in place, so drop + re-add. The default
-- name Postgres generates for an inline CHECK on column `mode` of table
-- `game_attempts` is `game_attempts_mode_check` — covered below. If your
-- DB has a different name (constraint was created with explicit naming),
-- run the inspector query at the bottom first.
--
-- Safe to re-run.
-- =============================================================================

ALTER TABLE game_attempts
  DROP CONSTRAINT IF EXISTS game_attempts_mode_check;

ALTER TABLE game_attempts
  ADD CONSTRAINT game_attempts_mode_check
  CHECK (mode IN ('trivia','fitb','who_am_i'));

-- If your environment named the constraint differently, find it with:
--   SELECT conname FROM pg_constraint
--    WHERE conrelid = 'game_attempts'::regclass AND contype = 'c';
-- Then replace `game_attempts_mode_check` above with the real name.
