-- =============================================================================
-- GRACE — Bible Games v2 schema upgrade
--
-- Two changes:
--
-- 1. Split Daily Challenge by difficulty: Easy / Medium / Hard. Each is its
--    own 10-question round, each unlocks independently per day. Streak fires
--    when the user completes *any* difficulty in the day — players aren't
--    forced to attempt Hard daily to keep their fire alive.
--
-- 2. Bulk-load ~25 more curated NKJV trivia questions so 10/category/day is
--    actually playable. Pre-Phase-5 floor; leader curation builds from here.
--
-- Safe to re-run.
-- =============================================================================

-- ---- COLUMNS: per-difficulty completion timestamps -------------------------
-- last_daily_challenge_at (legacy single column from v1) is kept but no
-- longer written by the client. Drop it in a follow-up after a few weeks
-- once we're sure nothing reads it.
ALTER TABLE game_user_stats
  ADD COLUMN IF NOT EXISTS last_daily_easy_at   TIMESTAMPTZ;
ALTER TABLE game_user_stats
  ADD COLUMN IF NOT EXISTS last_daily_medium_at TIMESTAMPTZ;
ALTER TABLE game_user_stats
  ADD COLUMN IF NOT EXISTS last_daily_hard_at   TIMESTAMPTZ;

-- ---- EXTRA SEED: ~25 questions to make 10/category/day playable -----------
INSERT INTO bible_questions (category, difficulty, question, options, correct_index, source_ref) VALUES
-- Easy (Old Testament + New Testament + Character)
('old_testament','easy','Who built the first temple in Jerusalem?',
 '["David","Solomon","Hezekiah","Josiah"]'::jsonb,1,'1 Kings 6'),
('old_testament','easy','Who was Adam''s wife?',
 '["Sarah","Eve","Rebekah","Rachel"]'::jsonb,1,'Genesis 2:22'),
('old_testament','easy','What instrument did David play to soothe King Saul?',
 '["Lyre","Harp","Trumpet","Drum"]'::jsonb,1,'1 Samuel 16:23'),
('new_testament','easy','What did Jesus turn water into at the wedding in Cana?',
 '["Bread","Wine","Oil","Honey"]'::jsonb,1,'John 2:9'),
('new_testament','easy','How many books are in the Bible?',
 '["50","60","66","72"]'::jsonb,2,'Canon'),
('new_testament','easy','Who wrote the book of Revelation?',
 '["Paul","Peter","John","Luke"]'::jsonb,2,'Revelation 1:1'),
('new_testament','easy','What were the first three words of the Bible?',
 '["God created earth","In the beginning","Let there be","Heavens and earth"]'::jsonb,1,'Genesis 1:1'),
('character','easy','Who was the first murderer in the Bible?',
 '["Cain","Abel","Esau","Lamech"]'::jsonb,0,'Genesis 4:8'),
('character','easy','Who was the father of Isaac?',
 '["Adam","Noah","Abraham","Jacob"]'::jsonb,2,'Genesis 21:3'),

-- Medium
('old_testament','medium','How many plagues did God send on Egypt?',
 '["Seven","Eight","Nine","Ten"]'::jsonb,3,'Exodus 7-12'),
('old_testament','medium','Who was sold into slavery by his brothers?',
 '["Benjamin","Joseph","Judah","Simeon"]'::jsonb,1,'Genesis 37:28'),
('old_testament','medium','Who was the strongest man in the Bible?',
 '["David","Goliath","Samson","Gideon"]'::jsonb,2,'Judges 16'),
('new_testament','medium','Who replaced Judas Iscariot among the apostles?',
 '["Stephen","Barnabas","Matthias","Silas"]'::jsonb,2,'Acts 1:26'),
('new_testament','medium','Where did Jesus give the Sermon on the Mount?',
 '["A mountainside","The Temple","A boat","Jerusalem"]'::jsonb,0,'Matthew 5:1'),
('new_testament','medium','Which apostle walked on water (briefly) toward Jesus?',
 '["John","Andrew","Peter","James"]'::jsonb,2,'Matthew 14:29'),
('character','medium','Who was the first king of Israel?',
 '["David","Saul","Solomon","Samuel"]'::jsonb,1,'1 Samuel 10:1'),
('character','medium','Which woman became David''s great-grandmother through marriage to Boaz?',
 '["Hannah","Naomi","Ruth","Bathsheba"]'::jsonb,2,'Ruth 4:13-22'),

-- Hard
('old_testament','hard','How many books are in the Old Testament?',
 '["27","36","39","46"]'::jsonb,2,'Canon'),
('old_testament','hard','How old was Moses when he died?',
 '["80","100","120","150"]'::jsonb,2,'Deuteronomy 34:7'),
('old_testament','hard','Which prophet was swallowed by a great fish?',
 '["Jonah","Hosea","Amos","Micah"]'::jsonb,0,'Jonah 1:17'),
('new_testament','hard','Who wrote the book of Acts?',
 '["Paul","Peter","Luke","John"]'::jsonb,2,'Acts 1:1'),
('new_testament','hard','Which apostle is called "the beloved disciple"?',
 '["Peter","James","John","Andrew"]'::jsonb,2,'John 13:23'),
('new_testament','hard','Who was the king of Judea when Jesus was born?',
 '["Augustus","Tiberius","Herod the Great","Pilate"]'::jsonb,2,'Matthew 2:1'),
('character','hard','What was the name of Abraham''s nephew who lived in Sodom?',
 '["Eliezer","Ishmael","Lot","Laban"]'::jsonb,2,'Genesis 13:12'),
('character','hard','Who was the mother of Samuel the prophet?',
 '["Sarah","Hannah","Rachel","Rebekah"]'::jsonb,1,'1 Samuel 1:20')
ON CONFLICT DO NOTHING;
