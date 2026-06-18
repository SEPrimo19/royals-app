
ALTER TABLE game_attempts
  ADD COLUMN IF NOT EXISTS character_id UUID
    REFERENCES bible_characters(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_game_attempts_character
  ON game_attempts (character_id) WHERE character_id IS NOT NULL;

