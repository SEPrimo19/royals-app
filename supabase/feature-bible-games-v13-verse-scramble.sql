-- =============================================================================
-- GRACE — Bible Games v13: "Verse Scramble" mode (v2, Practice-only)
--
-- Tap-to-place reconstruction of a scripture verse word by word. 5 verses
-- per round. Score:
--   • +30 pts per correctly assembled verse (150 base for 5)
--   • +10 perfect-first-try bonus per verse (zero wrong taps) → up to 200/round
--
-- Practice-only for v1, feeds monthly global leaderboard via game_attempts
-- (mode = 'verse_scramble').
--
-- Per the [[who-am-i-shipped]] new-mode checklist, this migration contains
-- all three things every new mode needs:
--   1. New content table        (bible_verse_scrambles)
--   2. Widen game_attempts.mode CHECK to add 'verse_scramble'
--   3. Add a per-content FK column on game_attempts (scramble_id)
--
-- Safe to re-run.
-- =============================================================================

-- ---- TABLE: bible_verse_scrambles ----------------------------------------
CREATE TABLE IF NOT EXISTS bible_verse_scrambles (
  id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  reference   TEXT NOT NULL,           -- e.g. "Psalm 23:1"
  text        TEXT NOT NULL,           -- the verse exactly as it should be assembled
  -- Stored word count helps the client filter long verses without scanning
  -- the text. Kept generated to stay in sync with text edits.
  word_count  INTEGER GENERATED ALWAYS AS (
    array_length(string_to_array(btrim(text), ' '), 1)
  ) STORED,
  is_active   BOOLEAN NOT NULL DEFAULT TRUE,
  created_by  UUID REFERENCES users(id),
  created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_bible_verse_scrambles_active
  ON bible_verse_scrambles (is_active, word_count) WHERE is_active = TRUE;

-- ---- RLS ------------------------------------------------------------------
ALTER TABLE bible_verse_scrambles ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "bible_verse_scrambles_select" ON bible_verse_scrambles;
CREATE POLICY "bible_verse_scrambles_select" ON bible_verse_scrambles
  FOR SELECT
  USING (auth.role() = 'authenticated');

DROP POLICY IF EXISTS "bible_verse_scrambles_insert" ON bible_verse_scrambles;
CREATE POLICY "bible_verse_scrambles_insert" ON bible_verse_scrambles
  FOR INSERT
  WITH CHECK (
    EXISTS (
      SELECT 1 FROM users u
       WHERE u.id = auth.uid()
         AND u.role IN ('cell_leader','youth_president','pastor','admin')
    )
  );

DROP POLICY IF EXISTS "bible_verse_scrambles_update" ON bible_verse_scrambles;
CREATE POLICY "bible_verse_scrambles_update" ON bible_verse_scrambles
  FOR UPDATE
  USING (
    EXISTS (
      SELECT 1 FROM users u
       WHERE u.id = auth.uid()
         AND u.role IN ('cell_leader','youth_president','pastor','admin')
    )
  );

DROP POLICY IF EXISTS "bible_verse_scrambles_delete" ON bible_verse_scrambles;
CREATE POLICY "bible_verse_scrambles_delete" ON bible_verse_scrambles
  FOR DELETE
  USING (
    EXISTS (
      SELECT 1 FROM users u
       WHERE u.id = auth.uid()
         AND u.role IN ('cell_leader','youth_president','pastor','admin')
    )
  );

-- ---- game_attempts: widen mode CHECK + add scramble_id FK ----------------
ALTER TABLE game_attempts
  DROP CONSTRAINT IF EXISTS game_attempts_mode_check;

ALTER TABLE game_attempts
  ADD CONSTRAINT game_attempts_mode_check
  CHECK (mode IN ('trivia','fitb','who_am_i','memory_match','verse_scramble'));

ALTER TABLE game_attempts
  ADD COLUMN IF NOT EXISTS scramble_id UUID
    REFERENCES bible_verse_scrambles(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_game_attempts_scramble
  ON game_attempts (scramble_id) WHERE scramble_id IS NOT NULL;

-- ---- SEED: 20 well-known short verses (≤12 words) ------------------------
INSERT INTO bible_verse_scrambles (reference, text) VALUES
('Psalm 23:1',         'The Lord is my shepherd I shall not want'),
('1 John 4:8',         'God is love'),
('John 11:35',         'Jesus wept'),
('Psalm 46:10',        'Be still and know that I am God'),
('Philippians 4:13',   'I can do all things through Christ who strengthens me'),
('Romans 8:31',        'If God is for us who can be against us'),
('John 14:6',          'I am the way the truth and the life'),
('Matthew 6:33',       'Seek first the kingdom of God and His righteousness'),
('Joshua 24:15',       'As for me and my house we will serve the Lord'),
('Proverbs 3:5',       'Trust in the Lord with all your heart'),
('1 Peter 5:7',        'Casting all your care upon Him for He cares for you'),
('Isaiah 41:10',       'Fear not for I am with you'),
('Matthew 28:6',       'He is not here for He is risen'),
('Genesis 1:1',        'In the beginning God created the heavens and the earth'),
('Psalm 119:105',      'Your word is a lamp to my feet'),
('Romans 6:23',        'The wages of sin is death'),
('John 3:30',          'He must increase but I must decrease'),
('James 4:7',          'Submit to God resist the devil and he will flee'),
('1 Thessalonians 5:17','Pray without ceasing'),
('Hebrews 13:8',       'Jesus Christ is the same yesterday today and forever')
ON CONFLICT DO NOTHING;

-- Sanity check (run manually):
--   SELECT count(*), avg(word_count) FROM bible_verse_scrambles WHERE is_active;
-- Expected: 20 rows, average ~8 words/verse.
