-- =============================================================================
-- GRACE — Bible Games v12: "Memory Cards" mode (v2, Practice-only)
--
-- Match Bible reference cards with their verse snippet. 6 pairs per board
-- (12 cards on a 3x4 grid). Untimed. Scoring:
--   • +10 pts per matched pair (max 60 for the board)
--   • +30 perfect-clear bonus if zero mismatched flips
--
-- Practice-only for v1 — feeds monthly global leaderboard automatically
-- via a `game_attempts` row (mode = 'memory_match').
--
-- Per the [[who-am-i-shipped]] memory, every new mode needs three things:
--   1. New content table          (memory_card_pairs)
--   2. Widen game_attempts.mode CHECK to include the new mode value
--   3. Add a per-content FK column on game_attempts (pair_id)
-- All three are in this single migration. Safe to re-run.
-- =============================================================================

-- ---- TABLE: memory_card_pairs --------------------------------------------
CREATE TABLE IF NOT EXISTS memory_card_pairs (
  id             UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  reference      TEXT NOT NULL,            -- e.g. "John 3:16"
  verse_snippet  TEXT NOT NULL,            -- short, scannable snippet for the card UI
  full_text      TEXT,                     -- optional full verse (shown on completion if useful)
  is_active      BOOLEAN NOT NULL DEFAULT TRUE,
  created_by     UUID REFERENCES users(id),
  created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_memory_card_pairs_active
  ON memory_card_pairs (is_active) WHERE is_active = TRUE;

-- ---- RLS ------------------------------------------------------------------
ALTER TABLE memory_card_pairs ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "memory_card_pairs_select" ON memory_card_pairs;
CREATE POLICY "memory_card_pairs_select" ON memory_card_pairs
  FOR SELECT
  USING (auth.role() = 'authenticated');

DROP POLICY IF EXISTS "memory_card_pairs_insert" ON memory_card_pairs;
CREATE POLICY "memory_card_pairs_insert" ON memory_card_pairs
  FOR INSERT
  WITH CHECK (
    EXISTS (
      SELECT 1 FROM users u
       WHERE u.id = auth.uid()
         AND u.role IN ('cell_leader','youth_president','pastor','admin')
    )
  );

DROP POLICY IF EXISTS "memory_card_pairs_update" ON memory_card_pairs;
CREATE POLICY "memory_card_pairs_update" ON memory_card_pairs
  FOR UPDATE
  USING (
    EXISTS (
      SELECT 1 FROM users u
       WHERE u.id = auth.uid()
         AND u.role IN ('cell_leader','youth_president','pastor','admin')
    )
  );

DROP POLICY IF EXISTS "memory_card_pairs_delete" ON memory_card_pairs;
CREATE POLICY "memory_card_pairs_delete" ON memory_card_pairs
  FOR DELETE
  USING (
    EXISTS (
      SELECT 1 FROM users u
       WHERE u.id = auth.uid()
         AND u.role IN ('cell_leader','youth_president','pastor','admin')
    )
  );

-- ---- game_attempts: widen mode CHECK + add pair_id FK --------------------
ALTER TABLE game_attempts
  DROP CONSTRAINT IF EXISTS game_attempts_mode_check;

ALTER TABLE game_attempts
  ADD CONSTRAINT game_attempts_mode_check
  CHECK (mode IN ('trivia','fitb','who_am_i','memory_match'));

ALTER TABLE game_attempts
  ADD COLUMN IF NOT EXISTS pair_id UUID
    REFERENCES memory_card_pairs(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_game_attempts_pair
  ON game_attempts (pair_id) WHERE pair_id IS NOT NULL;

-- ---- SEED: 30 verse pairs ------------------------------------------------
-- Snippets are deliberately short (≤ 60 chars) so they fit on a card
-- without wrapping awkwardly. Full text is kept for future "Completed!"
-- reveal screens.
INSERT INTO memory_card_pairs (reference, verse_snippet, full_text) VALUES
('John 3:16',
 'For God so loved the world',
 'For God so loved the world that He gave His only begotten Son, that whoever believes in Him should not perish but have everlasting life.'),
('Psalm 23:1',
 'The Lord is my shepherd',
 'The Lord is my shepherd; I shall not want.'),
('Proverbs 3:5',
 'Trust in the Lord with all your heart',
 'Trust in the Lord with all your heart, and lean not on your own understanding.'),
('Philippians 4:13',
 'I can do all things through Christ',
 'I can do all things through Christ who strengthens me.'),
('Romans 8:28',
 'All things work together for good',
 'And we know that all things work together for good to those who love God.'),
('Joshua 1:9',
 'Be strong and of good courage',
 'Be strong and of good courage; do not be afraid, nor be dismayed, for the Lord your God is with you wherever you go.'),
('Matthew 28:19',
 'Go and make disciples of all the nations',
 'Go therefore and make disciples of all the nations, baptizing them in the name of the Father and of the Son and of the Holy Spirit.'),
('Isaiah 40:31',
 'Those who wait on the Lord shall renew their strength',
 'But those who wait on the Lord shall renew their strength; they shall mount up with wings like eagles.'),
('Jeremiah 29:11',
 'For I know the thoughts I think toward you',
 'For I know the thoughts that I think toward you, says the Lord, thoughts of peace and not of evil.'),
('2 Timothy 1:7',
 'Not a spirit of fear, but of power',
 'For God has not given us a spirit of fear, but of power and of love and of a sound mind.'),
('Romans 12:2',
 'Be transformed by the renewing of your mind',
 'And do not be conformed to this world, but be transformed by the renewing of your mind.'),
('Matthew 6:33',
 'Seek first the kingdom of God',
 'But seek first the kingdom of God and His righteousness, and all these things shall be added to you.'),
('1 Corinthians 13:4',
 'Love suffers long and is kind',
 'Love suffers long and is kind; love does not envy; love does not parade itself, is not puffed up.'),
('John 14:6',
 'I am the way, the truth, and the life',
 'Jesus said to him, "I am the way, the truth, and the life. No one comes to the Father except through Me."'),
('Galatians 5:22',
 'The fruit of the Spirit is love',
 'But the fruit of the Spirit is love, joy, peace, longsuffering, kindness, goodness, faithfulness.'),
('Ephesians 2:8',
 'By grace you have been saved through faith',
 'For by grace you have been saved through faith, and that not of yourselves; it is the gift of God.'),
('Hebrews 11:1',
 'Faith is the substance of things hoped for',
 'Now faith is the substance of things hoped for, the evidence of things not seen.'),
('James 1:5',
 'If any of you lacks wisdom, let him ask of God',
 'If any of you lacks wisdom, let him ask of God, who gives to all liberally and without reproach.'),
('1 John 4:8',
 'God is love',
 'He who does not love does not know God, for God is love.'),
('Psalm 46:10',
 'Be still, and know that I am God',
 'Be still, and know that I am God; I will be exalted among the nations.'),
('Proverbs 22:6',
 'Train up a child in the way he should go',
 'Train up a child in the way he should go, and when he is old he will not depart from it.'),
('Isaiah 41:10',
 'Fear not, for I am with you',
 'Fear not, for I am with you; be not dismayed, for I am your God. I will strengthen you, yes, I will help you.'),
('Matthew 11:28',
 'Come to Me, all you who labor',
 'Come to Me, all you who labor and are heavy laden, and I will give you rest.'),
('Romans 6:23',
 'The wages of sin is death',
 'For the wages of sin is death, but the gift of God is eternal life in Christ Jesus our Lord.'),
('John 1:1',
 'In the beginning was the Word',
 'In the beginning was the Word, and the Word was with God, and the Word was God.'),
('Genesis 1:1',
 'In the beginning God created',
 'In the beginning God created the heavens and the earth.'),
('Acts 1:8',
 'You shall receive power',
 'But you shall receive power when the Holy Spirit has come upon you; and you shall be witnesses to Me.'),
('Psalm 119:105',
 'Your word is a lamp to my feet',
 'Your word is a lamp to my feet and a light to my path.'),
('Ephesians 6:11',
 'Put on the whole armor of God',
 'Put on the whole armor of God, that you may be able to stand against the wiles of the devil.'),
('1 Peter 5:7',
 'Cast all your care upon Him',
 'Casting all your care upon Him, for He cares for you.')
ON CONFLICT DO NOTHING;

-- Sanity check (run manually):
--   SELECT count(*) FROM memory_card_pairs WHERE is_active;
-- Expected: 30.
