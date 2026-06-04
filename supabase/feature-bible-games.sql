-- =============================================================================
-- GRACE — Feature: Bible Games v1
--
-- Two play modes in v1:
--   - "trivia"  — MCQ Daily Challenge (5 questions/day) + Practice
--   - "fitb"    — Fill-in-the-Blank against curated Bible passages
--
-- Scoring (locked):
--   easy = 10, medium = 20, hard = 30 (wrong = 0)
--   Daily streak bonus +5/day current streak, capped at +25.
--
-- Leaderboard: cell-group weekly only, Top 5, Monday reset.
-- See `bible-games-v1-design` memory doc for the locked design decisions.
-- Safe to re-run.
-- =============================================================================

-- ---- TRIVIA QUESTIONS ------------------------------------------------------
CREATE TABLE IF NOT EXISTS bible_questions (
  id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  category      TEXT NOT NULL CHECK (category IN ('old_testament','new_testament','character')),
  difficulty    TEXT NOT NULL CHECK (difficulty IN ('easy','medium','hard')),
  question      TEXT NOT NULL,
  options       JSONB NOT NULL,  -- array of 4 strings
  correct_index INTEGER NOT NULL CHECK (correct_index BETWEEN 0 AND 3),
  explanation   TEXT,
  source_ref    TEXT,            -- e.g. "Genesis 1:1"
  language      TEXT NOT NULL DEFAULT 'nkjv',
  is_active     BOOLEAN NOT NULL DEFAULT TRUE,
  created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_bible_questions_active
  ON bible_questions (is_active, difficulty);

-- ---- FILL-IN-THE-BLANK PASSAGES --------------------------------------------
-- Each passage has ONE pre-curated blank to keep v1 simple. v2 can add
-- multi-blank passages or auto-blanking.
CREATE TABLE IF NOT EXISTS bible_passages (
  id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  reference     TEXT NOT NULL,   -- "John 3:16"
  text          TEXT NOT NULL,   -- full verse text, with the blank word AS-IS
  blank_word    TEXT NOT NULL,   -- the word to remove + ask the user for
  distractors   JSONB NOT NULL,  -- array of 3 wrong-answer strings
  language      TEXT NOT NULL DEFAULT 'nkjv',
  is_active     BOOLEAN NOT NULL DEFAULT TRUE,
  created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_bible_passages_active ON bible_passages (is_active);

-- ---- GAME ATTEMPTS ---------------------------------------------------------
-- One row per question/passage answered. Used for scoring + leaderboard.
CREATE TABLE IF NOT EXISTS game_attempts (
  id             UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id        UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  mode           TEXT NOT NULL CHECK (mode IN ('trivia','fitb')),
  question_id    UUID REFERENCES bible_questions(id) ON DELETE SET NULL,
  passage_id     UUID REFERENCES bible_passages(id) ON DELETE SET NULL,
  correct        BOOLEAN NOT NULL,
  points_earned  INTEGER NOT NULL DEFAULT 0,
  -- Marks this attempt as part of the once-per-day Daily Challenge run
  -- (vs unlimited Practice). Only daily attempts feed the leaderboard.
  is_daily       BOOLEAN NOT NULL DEFAULT FALSE,
  played_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_game_attempts_user
  ON game_attempts (user_id, played_at DESC);
CREATE INDEX IF NOT EXISTS idx_game_attempts_leaderboard
  ON game_attempts (played_at DESC, is_daily) WHERE is_daily = TRUE;

-- ---- PER-USER STATS --------------------------------------------------------
-- Source of truth for streak + total points. Updated server-side when the
-- client posts a daily-challenge completion via [upsert_game_stats] below.
CREATE TABLE IF NOT EXISTS game_user_stats (
  user_id                   UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
  current_streak            INTEGER NOT NULL DEFAULT 0,
  longest_streak            INTEGER NOT NULL DEFAULT 0,
  total_points              BIGINT NOT NULL DEFAULT 0,
  last_played_at            TIMESTAMPTZ,
  last_daily_challenge_at   TIMESTAMPTZ
);

-- ---- WEEKLY GROUP LEADERBOARD VIEW -----------------------------------------
-- "This week" = since the most recent Monday 00:00 in the server's TZ.
-- date_trunc('week', NOW()) returns Monday 00:00 in Postgres by default.
-- Joins through users.group_id so the row carries the group context.
CREATE OR REPLACE VIEW weekly_group_leaderboard AS
SELECT
  a.user_id,
  u.name        AS user_name,
  u.group_id    AS group_id,
  SUM(a.points_earned)::INTEGER AS week_points,
  COUNT(*)      AS week_attempts
FROM game_attempts a
JOIN users u ON u.id = a.user_id
WHERE a.is_daily = TRUE
  AND a.played_at >= date_trunc('week', NOW())
GROUP BY a.user_id, u.name, u.group_id;

-- ---- RLS -------------------------------------------------------------------
ALTER TABLE bible_questions       ENABLE ROW LEVEL SECURITY;
ALTER TABLE bible_passages        ENABLE ROW LEVEL SECURITY;
ALTER TABLE game_attempts         ENABLE ROW LEVEL SECURITY;
ALTER TABLE game_user_stats       ENABLE ROW LEVEL SECURITY;

-- Content (questions + passages): anyone signed-in reads; only leaders
-- write. Curation screen lives behind that role gate.
DROP POLICY IF EXISTS "bq_select" ON bible_questions;
DROP POLICY IF EXISTS "bq_write"  ON bible_questions;
CREATE POLICY "bq_select" ON bible_questions
  FOR SELECT USING (auth.role() = 'authenticated');
CREATE POLICY "bq_write" ON bible_questions
  FOR ALL USING (
    EXISTS (
      SELECT 1 FROM users WHERE id = auth.uid()
        AND role IN ('cell_leader','youth_president','pastor','admin')
    )
  );

DROP POLICY IF EXISTS "bp_select" ON bible_passages;
DROP POLICY IF EXISTS "bp_write"  ON bible_passages;
CREATE POLICY "bp_select" ON bible_passages
  FOR SELECT USING (auth.role() = 'authenticated');
CREATE POLICY "bp_write" ON bible_passages
  FOR ALL USING (
    EXISTS (
      SELECT 1 FROM users WHERE id = auth.uid()
        AND role IN ('cell_leader','youth_president','pastor','admin')
    )
  );

-- Attempts: a user can read+write their own; leaders can also read their
-- mentees' attempts (powers the MemberDetail engagement view in v2).
DROP POLICY IF EXISTS "ga_select_self_or_leader" ON game_attempts;
DROP POLICY IF EXISTS "ga_insert_self"           ON game_attempts;
CREATE POLICY "ga_select_self_or_leader" ON game_attempts
  FOR SELECT USING (
    auth.uid() = user_id
    OR EXISTS (
      SELECT 1 FROM users
      WHERE id = auth.uid()
        AND role IN ('cell_leader','youth_president','pastor','admin')
    )
  );
CREATE POLICY "ga_insert_self" ON game_attempts
  FOR INSERT WITH CHECK (auth.uid() = user_id);

-- Stats: own-row CRUD; leaders read any.
DROP POLICY IF EXISTS "gus_select_self_or_leader" ON game_user_stats;
DROP POLICY IF EXISTS "gus_write_self"            ON game_user_stats;
CREATE POLICY "gus_select_self_or_leader" ON game_user_stats
  FOR SELECT USING (
    auth.uid() = user_id
    OR EXISTS (
      SELECT 1 FROM users
      WHERE id = auth.uid()
        AND role IN ('cell_leader','youth_president','pastor','admin')
    )
  );
CREATE POLICY "gus_write_self" ON game_user_stats
  FOR ALL USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

-- Views inherit base-table RLS in Postgres (game_attempts policy applies
-- when SELECTing through weekly_group_leaderboard). So a member only sees
-- their own row + leaders see everyone — same as the underlying table.

-- ---- SEED: trivia (20 questions across difficulties/categories) -----------
INSERT INTO bible_questions (category, difficulty, question, options, correct_index, source_ref) VALUES
('old_testament','easy','Who built an ark to survive the great flood?',
 '["Noah","Moses","Abraham","David"]'::jsonb,0,'Genesis 6'),
('old_testament','easy','Who led the Israelites out of Egypt?',
 '["Joshua","Moses","Aaron","Samuel"]'::jsonb,1,'Exodus 14'),
('old_testament','easy','Who defeated Goliath with a sling and a stone?',
 '["Samson","Saul","David","Jonathan"]'::jsonb,2,'1 Samuel 17'),
('old_testament','medium','In how many days did God create the world before resting?',
 '["Five","Six","Seven","Eight"]'::jsonb,1,'Genesis 1-2'),
('old_testament','medium','What was the name of Moses’ brother who became the first high priest?',
 '["Aaron","Caleb","Joshua","Eleazar"]'::jsonb,0,'Exodus 28'),
('old_testament','hard','How many years did the Israelites wander in the wilderness?',
 '["Twenty","Thirty","Forty","Seventy"]'::jsonb,2,'Numbers 14:33'),
('old_testament','hard','Which prophet was taken up to heaven in a whirlwind by a chariot of fire?',
 '["Elisha","Elijah","Isaiah","Ezekiel"]'::jsonb,1,'2 Kings 2:11'),

('new_testament','easy','In which town was Jesus born?',
 '["Nazareth","Jerusalem","Bethlehem","Capernaum"]'::jsonb,2,'Luke 2:4-7'),
('new_testament','easy','Who baptized Jesus in the Jordan River?',
 '["Peter","John the Baptist","Andrew","Matthew"]'::jsonb,1,'Matthew 3:13-17'),
('new_testament','easy','How many disciples did Jesus choose?',
 '["Seven","Ten","Twelve","Seventy"]'::jsonb,2,'Matthew 10:1-4'),
('new_testament','medium','At which wedding did Jesus perform His first recorded miracle?',
 '["Bethany","Cana","Capernaum","Nain"]'::jsonb,1,'John 2:1-11'),
('new_testament','medium','Who wrote most of the New Testament epistles?',
 '["Peter","Paul","John","James"]'::jsonb,1,'Acts 9; multiple letters'),
('new_testament','hard','Which book follows the Gospels and records the early church?',
 '["Romans","Acts","Hebrews","Revelation"]'::jsonb,1,'Acts 1:1'),
('new_testament','hard','How many days did Jesus appear to His disciples after the resurrection before ascending?',
 '["Three","Seven","Forty","Fifty"]'::jsonb,2,'Acts 1:3'),

('character','easy','Which young shepherd became the second king of Israel?',
 '["Saul","David","Solomon","Jonathan"]'::jsonb,1,'1 Samuel 16'),
('character','easy','Who was thrown into a den of lions but unharmed?',
 '["Daniel","Joseph","Joshua","Jeremiah"]'::jsonb,0,'Daniel 6'),
('character','medium','Which woman hid Israelite spies in Jericho?',
 '["Ruth","Rahab","Esther","Deborah"]'::jsonb,1,'Joshua 2'),
('character','medium','Who became queen of Persia and saved her people from genocide?',
 '["Ruth","Esther","Hannah","Miriam"]'::jsonb,1,'Esther 7'),
('character','hard','Which apostle was originally named Saul of Tarsus before his conversion?',
 '["Peter","Paul","Barnabas","Stephen"]'::jsonb,1,'Acts 9'),
('character','hard','Who was the father of John the Baptist?',
 '["Joseph","Zacharias","Simeon","Eli"]'::jsonb,1,'Luke 1:5-13')
ON CONFLICT DO NOTHING;

-- ---- SEED: Fill-in-the-Blank passages (8 verses, NKJV) --------------------
INSERT INTO bible_passages (reference, text, blank_word, distractors) VALUES
('John 3:16',
 'For God so loved the world that He gave His only begotten Son, that whoever believes in Him should not perish but have everlasting life.',
 'loved', '["judged","saved","sent"]'::jsonb),
('Psalm 23:1',
 'The Lord is my shepherd; I shall not want.',
 'shepherd', '["keeper","strength","portion"]'::jsonb),
('Proverbs 3:5',
 'Trust in the Lord with all your heart, and lean not on your own understanding.',
 'heart', '["soul","mind","strength"]'::jsonb),
('Philippians 4:13',
 'I can do all things through Christ who strengthens me.',
 'strengthens', '["loves","saves","sends"]'::jsonb),
('Romans 8:28',
 'And we know that all things work together for good to those who love God, to those who are the called according to His purpose.',
 'good', '["peace","glory","joy"]'::jsonb),
('Joshua 1:9',
 'Be strong and of good courage; do not be afraid, nor be dismayed, for the Lord your God is with you wherever you go.',
 'courage', '["faith","heart","spirit"]'::jsonb),
('Matthew 28:19',
 'Go therefore and make disciples of all the nations, baptizing them in the name of the Father and of the Son and of the Holy Spirit.',
 'disciples', '["servants","believers","followers"]'::jsonb),
('Isaiah 40:31',
 'But those who wait on the Lord shall renew their strength; they shall mount up with wings like eagles.',
 'strength', '["spirit","courage","vision"]'::jsonb)
ON CONFLICT DO NOTHING;

