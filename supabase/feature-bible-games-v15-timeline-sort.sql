-- =============================================================================
-- GRACE — Bible Games v15: "Timeline Sorting" mode (v2, Practice-only)
--
-- Tap-to-place chronological reconstruction. Each puzzle = 5 random events
-- the player taps in order from earliest → latest. 3 puzzles per round.
--
-- Scoring:
--   +40 pts per correctly sorted puzzle
--   +20 perfect-no-undo bonus per puzzle (zero wrong taps)
--   → up to 60/puzzle · 180/round
--
-- Practice-only for v1. Feeds the monthly global leaderboard via
-- game_attempts (mode = 'timeline_sort').
--
-- Unlike Who Am I? / Memory Cards / Verse Scramble, Timeline doesn't add
-- a content FK column on game_attempts: each puzzle is a dynamically
-- chosen SET of events, not a single curated row, so there's nothing
-- single-id to reference. game_attempts rows for timeline_sort just leave
-- all content FK columns null — fine because leaderboard / monthly sum
-- aggregate by user, not by content.
--
-- Per the [[who-am-i-shipped]] checklist, we still MUST widen the mode
-- CHECK so the inserts aren't silently rejected.
--
-- Safe to re-run.
-- =============================================================================

-- ---- TABLE: bible_events -------------------------------------------------
CREATE TABLE IF NOT EXISTS bible_events (
  id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  title               TEXT NOT NULL,
  description         TEXT,
  -- Canonical sort key. Lower number = earlier in Bible history. Spaced
  -- by 100 so future events can slot in between without renumbering.
  chronological_order INTEGER NOT NULL,
  -- Approximate date label shown in the reveal screen (e.g. "~1500 BC").
  -- Optional — Creation, Fall, etc. don't have meaningful dates.
  approx_year_text    TEXT,
  source_ref          TEXT,
  is_active           BOOLEAN NOT NULL DEFAULT TRUE,
  created_by          UUID REFERENCES users(id),
  created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_bible_events_active
  ON bible_events (is_active, chronological_order) WHERE is_active = TRUE;

-- ---- RLS ------------------------------------------------------------------
ALTER TABLE bible_events ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "bible_events_select" ON bible_events;
CREATE POLICY "bible_events_select" ON bible_events
  FOR SELECT
  USING (auth.role() = 'authenticated');

DROP POLICY IF EXISTS "bible_events_insert" ON bible_events;
CREATE POLICY "bible_events_insert" ON bible_events
  FOR INSERT
  WITH CHECK (
    EXISTS (
      SELECT 1 FROM users u
       WHERE u.id = auth.uid()
         AND u.role IN ('cell_leader','youth_president','pastor','admin')
    )
  );

DROP POLICY IF EXISTS "bible_events_update" ON bible_events;
CREATE POLICY "bible_events_update" ON bible_events
  FOR UPDATE
  USING (
    EXISTS (
      SELECT 1 FROM users u
       WHERE u.id = auth.uid()
         AND u.role IN ('cell_leader','youth_president','pastor','admin')
    )
  );

DROP POLICY IF EXISTS "bible_events_delete" ON bible_events;
CREATE POLICY "bible_events_delete" ON bible_events
  FOR DELETE
  USING (
    EXISTS (
      SELECT 1 FROM users u
       WHERE u.id = auth.uid()
         AND u.role IN ('cell_leader','youth_president','pastor','admin')
    )
  );

-- ---- game_attempts: widen mode CHECK only (no FK column for this mode) --
ALTER TABLE game_attempts
  DROP CONSTRAINT IF EXISTS game_attempts_mode_check;

ALTER TABLE game_attempts
  ADD CONSTRAINT game_attempts_mode_check
  CHECK (mode IN (
    'trivia','fitb','who_am_i','memory_match','verse_scramble','timeline_sort'
  ));

-- ---- SEED: ~33 events spanning Creation → early Church -------------------
INSERT INTO bible_events
  (title, description, chronological_order, approx_year_text, source_ref)
VALUES
('Creation',                'God creates the heavens, earth, and all life.',
   100,  null,            'Genesis 1-2'),
('The Fall in Eden',        'Adam and Eve eat the forbidden fruit and are exiled.',
   200,  null,            'Genesis 3'),
('Cain kills Abel',         'The first murder.',
   300,  null,            'Genesis 4'),
('Noah and the Flood',      'God floods the earth; Noah and his family are saved.',
   400,  '~2300 BC',      'Genesis 6-9'),
('Tower of Babel',          'God scatters humanity by confusing their language.',
   500,  null,            'Genesis 11'),
('Call of Abraham',         'God calls Abram to leave Ur and promises a great nation.',
   600,  '~2000 BC',      'Genesis 12'),
('Birth of Isaac',          'Sarah bears Abraham a son in their old age.',
   700,  null,            'Genesis 21'),
('Jacob renamed Israel',    'Jacob wrestles with God and receives the name Israel.',
   800,  null,            'Genesis 32'),
('Joseph sold into Egypt',  'His brothers sell him to traders; he rises to second over Egypt.',
   900,  null,            'Genesis 37-41'),
('Moses born in Egypt',     'A Hebrew baby is hidden in the Nile and raised by Pharaoh''s daughter.',
   1000, '~1525 BC',      'Exodus 2'),
('The Exodus from Egypt',   'God delivers Israel through ten plagues; they cross the Red Sea.',
   1100, '~1446 BC',      'Exodus 7-14'),
('Ten Commandments at Sinai','God gives Moses the Law on Mount Sinai.',
   1200, '~1446 BC',      'Exodus 19-20'),
('Joshua takes Jericho',    'The walls fall when Israel marches around and shouts.',
   1300, '~1406 BC',      'Joshua 6'),
('Period of the Judges',    'Israel cycles between sin, oppression, and deliverance.',
   1400, null,            'Judges'),
('Samuel anoints Saul',     'Israel demands a king; Samuel anoints Saul.',
   1500, '~1050 BC',      '1 Samuel 10'),
('David defeats Goliath',   'Young David slays the Philistine giant.',
   1600, null,            '1 Samuel 17'),
('Solomon builds the Temple','The first temple is completed in Jerusalem.',
   1700, '~960 BC',       '1 Kings 6'),
('The kingdom divides',     'After Solomon, Israel splits into Northern and Southern kingdoms.',
   1800, '~930 BC',       '1 Kings 12'),
('Elijah on Mount Carmel',  'Elijah confronts the prophets of Baal; fire falls from heaven.',
   1900, '~860 BC',       '1 Kings 18'),
('Assyria conquers Israel', 'The Northern Kingdom falls and the ten tribes are scattered.',
   2000, '722 BC',        '2 Kings 17'),
('Jerusalem and temple destroyed','Nebuchadnezzar burns Jerusalem; Judah goes to Babylon.',
   2100, '586 BC',        '2 Kings 25'),
('Daniel in Babylon',       'Daniel serves under Babylonian and Persian kings.',
   2200, '~605-530 BC',   'Daniel 1-6'),
('Return under Zerubbabel', 'The first exiles return to rebuild the temple.',
   2300, '~538 BC',       'Ezra 1-3'),
('Ezra returns to Jerusalem','Ezra restores the Law among the returned exiles.',
   2400, '~458 BC',       'Ezra 7'),
('Nehemiah rebuilds the walls','Jerusalem''s walls are rebuilt in 52 days.',
   2500, '~445 BC',       'Nehemiah 6'),
('Esther saves the Jews',   'Queen Esther exposes Haman''s plot.',
   2600, '~474 BC',       'Esther 7'),
('Birth of Jesus in Bethlehem','The promised Messiah is born.',
   3000, '~4 BC',         'Luke 2'),
('Baptism of Jesus',        'John baptizes Jesus in the Jordan.',
   3100, '~AD 27',        'Matthew 3'),
('Crucifixion and Resurrection','Jesus is crucified, buried, and rises on the third day.',
   3200, '~AD 30',        'Matthew 27-28'),
('Day of Pentecost',        'The Holy Spirit is poured out; the Church begins.',
   3300, '~AD 30',        'Acts 2'),
('Conversion of Saul',      'Jesus appears to Saul on the road to Damascus.',
   3400, '~AD 34',        'Acts 9'),
('Council of Jerusalem',    'The apostles affirm salvation by grace for Gentiles.',
   3500, '~AD 50',        'Acts 15'),
('John''s Revelation on Patmos','The exiled apostle receives the visions of Revelation.',
   3600, '~AD 95',        'Revelation 1')
ON CONFLICT DO NOTHING;

-- Sanity check (run manually):
--   SELECT count(*) FROM bible_events WHERE is_active;
-- Expected: 33 rows.
