
ALTER TABLE game_user_stats
  ADD COLUMN IF NOT EXISTS last_daily_fitb_at TIMESTAMPTZ;
