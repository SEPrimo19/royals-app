
CREATE TABLE IF NOT EXISTS bible_characters (
  id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  name          TEXT NOT NULL,
  category      TEXT NOT NULL CHECK (category IN ('old_testament','new_testament','character')),
  difficulty    TEXT NOT NULL CHECK (difficulty IN ('easy','medium','hard')),
  clue_1        TEXT NOT NULL,
  clue_2        TEXT NOT NULL,
  clue_3        TEXT NOT NULL,
  clue_4        TEXT NOT NULL,
  distractors   JSONB NOT NULL,
  source_ref    TEXT,
  explanation   TEXT,
  is_active     BOOLEAN NOT NULL DEFAULT TRUE,
  created_by    UUID REFERENCES users(id),
  created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_bible_characters_active
  ON bible_characters(is_active) WHERE is_active = TRUE;

ALTER TABLE bible_characters ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "bible_characters_select" ON bible_characters;
CREATE POLICY "bible_characters_select" ON bible_characters
  FOR SELECT
  USING (auth.role() = 'authenticated');

DROP POLICY IF EXISTS "bible_characters_insert" ON bible_characters;
CREATE POLICY "bible_characters_insert" ON bible_characters
  FOR INSERT
  WITH CHECK (
    EXISTS (
      SELECT 1 FROM users u
       WHERE u.id = auth.uid()
         AND u.role IN ('cell_leader','youth_president','pastor','admin')
    )
  );

DROP POLICY IF EXISTS "bible_characters_update" ON bible_characters;
CREATE POLICY "bible_characters_update" ON bible_characters
  FOR UPDATE
  USING (
    EXISTS (
      SELECT 1 FROM users u
       WHERE u.id = auth.uid()
         AND u.role IN ('cell_leader','youth_president','pastor','admin')
    )
  );

DROP POLICY IF EXISTS "bible_characters_delete" ON bible_characters;
CREATE POLICY "bible_characters_delete" ON bible_characters
  FOR DELETE
  USING (
    EXISTS (
      SELECT 1 FROM users u
       WHERE u.id = auth.uid()
         AND u.role IN ('cell_leader','youth_president','pastor','admin')
    )
  );

INSERT INTO bible_characters
  (name, category, difficulty, clue_1, clue_2, clue_3, clue_4, distractors, source_ref, explanation)
VALUES

('Noah','old_testament','easy',
 'I built something massive at God''s command.',
 'It took me about 100 years to finish the build.',
 'My boat carried pairs of every animal.',
 'After the flood, I saw the first rainbow.',
 '["Moses","Abraham","Joshua"]'::jsonb,
 'Genesis 6-9',
 'God commanded Noah to build the ark to preserve life through the great flood; the rainbow was the covenant sign afterwards.'),
('Moses','old_testament','easy',
 'I was hidden in a basket as a baby.',
 'I tended sheep until a bush stopped me cold.',
 'I parted a sea so my people could walk through.',
 'I carried stone tablets down a mountain.',
 '["Aaron","Joshua","Samuel"]'::jsonb,
 'Exodus 2-20',
 'Moses led Israel out of Egypt and received the Ten Commandments on Mount Sinai.'),
('David','old_testament','easy',
 'I was the youngest of eight brothers.',
 'I tended my father''s sheep before I tended a nation.',
 'I dropped a giant with a single stone.',
 'I became king, and Psalms came from my heart.',
 '["Saul","Solomon","Jonathan"]'::jsonb,
 '1 Samuel 16-17',
 'David, son of Jesse, killed Goliath as a youth and later became Israel''s second king and a psalmist.'),
('Adam','old_testament','easy',
 'I was the first of my kind.',
 'God formed me from the dust.',
 'I named every animal that walked by.',
 'My wife and I lost a garden because of one fruit.',
 '["Noah","Cain","Seth"]'::jsonb,
 'Genesis 2-3',
 'Adam, the first man, was formed from the dust; he and Eve were expelled from Eden after eating the forbidden fruit.'),
('Jonah','old_testament','easy',
 'I tried to run away from God''s mission for me.',
 'A storm came because of my disobedience.',
 'I spent three days in the belly of a great fish.',
 'I finally preached to Nineveh and they repented.',
 '["Daniel","Elijah","Amos"]'::jsonb,
 'Jonah 1-3',
 'Jonah fled God''s call to Nineveh; a great fish swallowed him, then he finally preached and the city repented.'),

('Mary (mother of Jesus)','new_testament','easy',
 'An angel told me I was favored by God.',
 'I was betrothed to a carpenter when I conceived.',
 'I gave birth in a stable in Bethlehem.',
 'I sang a song magnifying the Lord.',
 '["Elizabeth","Anna","Mary Magdalene"]'::jsonb,
 'Luke 1-2',
 'Mary, the mother of Jesus, conceived by the Holy Spirit; her song is known as the Magnificat.'),
('John the Baptist','new_testament','easy',
 'My father Zacharias was struck mute before my birth.',
 'I lived in the wilderness and wore camel''s hair.',
 'I ate locusts and wild honey.',
 'I baptized my own cousin in the Jordan.',
 '["Peter","Andrew","John (apostle)"]'::jsonb,
 'Matthew 3; Luke 1',
 'John the Baptist prepared the way for Jesus; he baptized Him in the Jordan, and his father was Zacharias.'),
('Peter','new_testament','easy',
 'I was a fisherman before Jesus called me.',
 'Jesus gave me a new name meaning "rock".',
 'I walked on water briefly — then began to sink.',
 'I denied Jesus three times before the rooster crowed.',
 '["Andrew","John (apostle)","James"]'::jsonb,
 'Matthew 4; 14; 26',
 'Peter (Simon) was renamed by Jesus and famously walked on water; he later preached the first Pentecost sermon.'),

('Abraham','character','easy',
 'God called me to leave my homeland.',
 'I was 100 years old when my son of promise was born.',
 'I almost sacrificed my son on Mount Moriah.',
 'I am called the father of all who believe.',
 '["Isaac","Jacob","Lot"]'::jsonb,
 'Genesis 12-22',
 'Abraham left Ur, fathered Isaac at age 100, and was tested at Moriah; Scripture calls him the father of faith.'),
('Eve','character','easy',
 'I was formed from a rib.',
 'I am called the mother of all living.',
 'A serpent deceived me with a question.',
 'I gave my husband a forbidden fruit.',
 '["Sarah","Rebekah","Rachel"]'::jsonb,
 'Genesis 2-3',
 'Eve was formed from Adam''s rib; deceived by the serpent, she ate from the forbidden tree and shared it with Adam.'),

('Joseph (son of Jacob)','old_testament','medium',
 'My father gave me a coat of many colors.',
 'My brothers sold me into slavery for jealousy.',
 'I interpreted dreams about famine and plenty.',
 'I became second only to Pharaoh in Egypt.',
 '["Benjamin","Judah","Joshua"]'::jsonb,
 'Genesis 37-41',
 'Joseph was sold by his brothers, became Pharaoh''s right-hand after interpreting his dreams, and saved Egypt and his family.'),
('Daniel','old_testament','medium',
 'I refused to defile myself with the king''s food.',
 'I interpreted dreams for Babylonian kings.',
 'I prayed three times a day toward Jerusalem.',
 'I slept among lions and was unharmed.',
 '["Shadrach","Mordecai","Nehemiah"]'::jsonb,
 'Daniel 1-6',
 'Daniel served in Babylon, interpreted dreams for Nebuchadnezzar, and was preserved in the lions'' den under Darius.'),
('Elijah','old_testament','medium',
 'Ravens fed me at a brook called Cherith.',
 'I confronted 450 prophets on a mountain top.',
 'Fire fell from heaven on my soaked altar.',
 'I never died — a whirlwind took me up.',
 '["Elisha","Isaiah","Moses"]'::jsonb,
 '1 Kings 17-19; 2 Kings 2',
 'Elijah challenged Baal''s prophets on Mount Carmel and was taken up to heaven in a whirlwind without dying.'),

('Paul','new_testament','medium',
 'I once breathed threats against the church.',
 'A blinding light stopped me on a road.',
 'I wrote letters to churches across the empire.',
 'I was shipwrecked on the way to Rome.',
 '["Peter","Stephen","Silas"]'::jsonb,
 'Acts 9; 27',
 'Saul of Tarsus persecuted Christians until Jesus appeared to him on the Damascus road; he became Paul, apostle to the Gentiles.'),
('Mary Magdalene','new_testament','medium',
 'Jesus cast seven demons out of me.',
 'I followed Jesus and helped support His ministry.',
 'I watched the crucifixion from a distance.',
 'I was the first person to see Him risen.',
 '["Mary (mother of Jesus)","Martha","Joanna"]'::jsonb,
 'Luke 8:2; John 20:14-18',
 'Mary Magdalene was delivered from seven demons and became the first witness of the risen Christ.'),
('Thomas','new_testament','medium',
 'I am one of the twelve apostles.',
 'I refused to believe just because the others said so.',
 'I demanded to see the nail prints myself.',
 'When I finally saw Him, I cried "My Lord and my God!"',
 '["Andrew","Bartholomew","Matthew"]'::jsonb,
 'John 20:24-28',
 'Thomas (called Didymus, "the Twin") doubted the resurrection until Jesus appeared and showed him His wounds.'),

('Sarah','character','medium',
 'I was beautiful in my old age.',
 'I laughed when angels told me what God would do.',
 'I waited until 90 to hold my own child.',
 'My son was the child of God''s promise.',
 '["Hagar","Rebekah","Rachel"]'::jsonb,
 'Genesis 17-21',
 'Sarah, wife of Abraham, gave birth to Isaac at age 90 after laughing at the angels'' announcement.'),
('Ruth','character','medium',
 'I was a Moabite who refused to leave my mother-in-law.',
 'I gleaned grain in the fields of a kind landowner.',
 'My loyalty became famous: "your people shall be my people".',
 'My great-grandson became Israel''s king.',
 '["Naomi","Orpah","Hannah"]'::jsonb,
 'Ruth 1-4',
 'Ruth the Moabitess stayed with Naomi, married Boaz, and became the great-grandmother of King David.'),
('Samson','character','medium',
 'I was a Nazirite from birth — no razor touched my head.',
 'I tore a young lion apart with my bare hands.',
 'I killed a thousand men with the jawbone of a donkey.',
 'A haircut from Delilah took my strength away.',
 '["Gideon","Jephthah","Othniel"]'::jsonb,
 'Judges 13-16',
 'Samson, judge of Israel, drew supernatural strength from his Nazirite vow until Delilah''s deception led to his capture.'),

('Caleb','old_testament','hard',
 'I was one of 12 spies who scouted Canaan.',
 'Only Joshua and I believed God would give us the land.',
 'I waited 45 years for the inheritance I was promised.',
 'At 85 I still asked for the hill country.',
 '["Joshua","Othniel","Jephthah"]'::jsonb,
 'Numbers 13-14; Joshua 14',
 'Caleb, with Joshua, was one of two faithful spies; he received Hebron as inheritance at age 85.'),
('Nehemiah','old_testament','hard',
 'I was cupbearer to the king of Persia.',
 'I wept when I heard Jerusalem''s walls were broken.',
 'I led a 52-day wall rebuilding under armed guard.',
 'I prayed before I answered the king.',
 '["Ezra","Mordecai","Zerubbabel"]'::jsonb,
 'Nehemiah 1-6',
 'Nehemiah, cupbearer to Artaxerxes, led the return that rebuilt Jerusalem''s walls in just 52 days.'),
('Hosea','old_testament','hard',
 'God told me to marry an unfaithful woman.',
 'My marriage was a living picture of Israel.',
 'I named my children with prophetic messages.',
 'I prophesied to the northern kingdom about restoration.',
 '["Amos","Joel","Micah"]'::jsonb,
 'Hosea 1-3',
 'Hosea married Gomer at God''s command as a living parable of Israel''s spiritual adultery and God''s faithful love.'),
('Jael','old_testament','hard',
 'A Canaanite general fled into my tent thinking he was safe.',
 'I welcomed him with milk and a covering.',
 'While he slept I picked up a tent peg.',
 'Deborah''s song praises me as blessed among women.',
 '["Deborah","Esther","Hannah"]'::jsonb,
 'Judges 4:17-22',
 'Jael, wife of Heber, drove a tent peg through the temple of the fleeing commander Sisera and ended the battle.'),

('Cornelius','new_testament','hard',
 'I was a Roman centurion of the Italian Regiment.',
 'I feared God and gave alms generously.',
 'An angel told me to send for a fisherman in Joppa.',
 'My household became the first Gentile Christians baptized.',
 '["Julius","Felix","Festus"]'::jsonb,
 'Acts 10',
 'Cornelius, a God-fearing centurion in Caesarea, became the first Gentile convert after Peter''s vision sent him there.'),
('Lydia','new_testament','hard',
 'I sold purple cloth at a riverside in Philippi.',
 'I worshiped God before I ever heard the gospel.',
 'Paul baptized me and my whole household.',
 'I begged Paul to stay at my house.',
 '["Priscilla","Phoebe","Damaris"]'::jsonb,
 'Acts 16:14-15',
 'Lydia of Thyatira, a seller of purple, was Paul''s first European convert at Philippi.'),
('Onesimus','new_testament','hard',
 'I was a runaway slave who met Paul in prison.',
 'My name means "useful" but I had been unprofitable.',
 'Paul sent me back to my master with a letter.',
 'My master''s name was Philemon.',
 '["Tychicus","Epaphras","Trophimus"]'::jsonb,
 'Philemon 1:10-16',
 'Onesimus, a runaway slave converted under Paul''s ministry, was sent back to his master Philemon as a beloved brother.'),
('Stephen','new_testament','hard',
 'I was one of the first seven deacons.',
 'I was full of faith, grace, and the Holy Spirit.',
 'I gave a fiery sermon retelling Israel''s history.',
 'I saw heaven open as I was being stoned.',
 '["Philip","Barnabas","Silas"]'::jsonb,
 'Acts 6-7',
 'Stephen, the first Christian martyr, saw the Son of Man standing at God''s right hand as he was stoned to death.'),

('Melchizedek','character','hard',
 'I was a priest of God Most High before the law was given.',
 'I was king of a city later called Jerusalem.',
 'I brought out bread and wine to bless a victorious patriarch.',
 'Abraham gave me a tenth of everything.',
 '["Aaron","Eli","Zadok"]'::jsonb,
 'Genesis 14:18-20; Hebrews 7',
 'Melchizedek was priest-king of Salem; Abraham tithed to him, and Hebrews uses him as a type of Christ''s eternal priesthood.'),
('Huldah','character','hard',
 'I was a prophetess in Jerusalem.',
 'I lived during the reign of King Josiah.',
 'The king''s officials sought me out for counsel.',
 'I confirmed the words of the rediscovered Book of the Law.',
 '["Deborah","Miriam","Anna"]'::jsonb,
 '2 Kings 22:14',
 'Huldah the prophetess confirmed to Josiah''s envoys the warnings of the newly-found Book of the Law.'),
('Apollos','character','hard',
 'I was a Jew from Alexandria, eloquent and mighty in the Scriptures.',
 'I taught accurately but only knew John''s baptism.',
 'Priscilla and Aquila took me aside and explained the way of God more accurately.',
 'I powerfully refuted Jews in Achaia.',
 '["Aquila","Sosthenes","Demetrius"]'::jsonb,
 'Acts 18:24-28',
 'Apollos, an eloquent Alexandrian Jew, was further taught by Priscilla and Aquila and became a powerful gospel preacher in Corinth.')

ON CONFLICT DO NOTHING;

