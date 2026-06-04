-- =============================================================================
-- GRACE — Bible Games v3: Daily Verse (Fill-in-the-Blank) gating
--
-- Adds last_daily_fitb_at so the Daily Verse round has its own 24h unlock,
-- independent of the three Easy/Medium/Hard trivia dailies. Streak fires on
-- the FIRST daily completion of the day (any of: easy/medium/hard/fitb).
--
-- Safe to re-run.
-- =============================================================================

ALTER TABLE game_user_stats
  ADD COLUMN IF NOT EXISTS last_daily_fitb_at TIMESTAMPTZ;
