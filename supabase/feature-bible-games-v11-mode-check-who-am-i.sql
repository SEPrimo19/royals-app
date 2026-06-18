
ALTER TABLE game_attempts
  DROP CONSTRAINT IF EXISTS game_attempts_mode_check;

ALTER TABLE game_attempts
  ADD CONSTRAINT game_attempts_mode_check
  CHECK (mode IN ('trivia','fitb','who_am_i'));

