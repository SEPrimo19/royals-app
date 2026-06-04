-- =============================================================================
-- GRACE — Bible Games v10: add character_id FK to game_attempts
--
-- v9 shipped Who Am I? and the ViewModel tried to stuff the bible_characters
-- id into game_attempts.question_id. That column has an FK to bible_questions,
-- so every Who-Am-I insert silently failed the FK check — points never
-- landed and the monthly leaderboard ignored them.
--
-- Fix: add a peer column character_id REFERENCES bible_characters(id), then
-- the client writes the right column based on mode.
--
-- Safe to re-run.
-- =============================================================================

ALTER TABLE game_attempts
  ADD COLUMN IF NOT EXISTS character_id UUID
    REFERENCES bible_characters(id) ON DELETE SET NULL;

-- Helpful for any future "characters answered" leader analytics.
CREATE INDEX IF NOT EXISTS idx_game_attempts_character
  ON game_attempts (character_id) WHERE character_id IS NOT NULL;

-- Sanity check (run manually):
--   \d game_attempts
-- Expect to see character_id in the column list with an FK to bible_characters.
