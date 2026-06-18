
CREATE TABLE IF NOT EXISTS weekly_meditations (
  id                    UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  week_number           INTEGER NOT NULL,
  start_date            DATE NOT NULL,
  end_date              DATE NOT NULL,
  theme                 TEXT NOT NULL CHECK (
    theme IN ('JESUS','EDUCATION','FAMILY','FRIENDS','CHURCH','RELATIONSHIPS')
  ),
  title                 TEXT NOT NULL,
  scripture_ref         TEXT NOT NULL,
  scripture_text        TEXT NOT NULL,
  reflection_prompt     TEXT NOT NULL,
  further_reading_label TEXT,
  further_reading_url   TEXT,
  is_active             BOOLEAN NOT NULL DEFAULT TRUE,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_weekly_meditations_dates
  ON weekly_meditations (start_date, end_date) WHERE is_active = TRUE;

CREATE UNIQUE INDEX IF NOT EXISTS uq_weekly_meditations_week_year
  ON weekly_meditations (week_number, EXTRACT(YEAR FROM start_date));

ALTER TABLE weekly_meditations ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "meditation_select" ON weekly_meditations;
DROP POLICY IF EXISTS "meditation_insert" ON weekly_meditations;
DROP POLICY IF EXISTS "meditation_update" ON weekly_meditations;
DROP POLICY IF EXISTS "meditation_delete" ON weekly_meditations;

CREATE POLICY "meditation_select" ON weekly_meditations
  FOR SELECT USING (auth.role() = 'authenticated');

CREATE POLICY "meditation_insert" ON weekly_meditations
  FOR INSERT WITH CHECK (
    EXISTS (
      SELECT 1 FROM users
      WHERE id = auth.uid()
        AND role IN ('youth_president','pastor','admin')
    )
  );

CREATE POLICY "meditation_update" ON weekly_meditations
  FOR UPDATE USING (
    EXISTS (
      SELECT 1 FROM users
      WHERE id = auth.uid()
        AND role IN ('youth_president','pastor','admin')
    )
  );

CREATE POLICY "meditation_delete" ON weekly_meditations
  FOR DELETE USING (
    EXISTS (
      SELECT 1 FROM users
      WHERE id = auth.uid()
        AND role IN ('youth_president','pastor','admin')
    )
  );


CREATE TABLE IF NOT EXISTS user_meditation_submissions (
  id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  meditation_id   UUID NOT NULL REFERENCES weekly_meditations(id) ON DELETE CASCADE,
  reflection_text TEXT NOT NULL CHECK (length(reflection_text) > 0),
  submitted_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE (user_id, meditation_id)
);

CREATE INDEX IF NOT EXISTS idx_meditation_subs_user
  ON user_meditation_submissions (user_id, submitted_at DESC);
CREATE INDEX IF NOT EXISTS idx_meditation_subs_meditation
  ON user_meditation_submissions (meditation_id);

ALTER TABLE user_meditation_submissions ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "meditation_sub_select" ON user_meditation_submissions;
DROP POLICY IF EXISTS "meditation_sub_insert" ON user_meditation_submissions;
DROP POLICY IF EXISTS "meditation_sub_update" ON user_meditation_submissions;
DROP POLICY IF EXISTS "meditation_sub_delete" ON user_meditation_submissions;

CREATE POLICY "meditation_sub_select" ON user_meditation_submissions
  FOR SELECT USING (
    auth.uid() = user_id
    OR EXISTS (
      SELECT 1 FROM users me
      JOIN users target ON target.id = user_meditation_submissions.user_id
      WHERE me.id = auth.uid()
        AND (
          me.role IN ('youth_president','pastor','admin')
          OR (me.role = 'cell_leader'
              AND me.group_id IS NOT NULL
              AND me.group_id = target.group_id)
        )
    )
  );

CREATE POLICY "meditation_sub_insert" ON user_meditation_submissions
  FOR INSERT WITH CHECK (auth.uid() = user_id);

CREATE POLICY "meditation_sub_update" ON user_meditation_submissions
  FOR UPDATE USING (auth.uid() = user_id);

CREATE POLICY "meditation_sub_delete" ON user_meditation_submissions
  FOR DELETE USING (auth.uid() = user_id);

CREATE OR REPLACE FUNCTION touch_meditation_submission_updated_at()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
  NEW.updated_at := NOW();
  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_meditation_sub_updated_at
  ON user_meditation_submissions;
CREATE TRIGGER trg_meditation_sub_updated_at
  BEFORE UPDATE ON user_meditation_submissions
  FOR EACH ROW EXECUTE FUNCTION touch_meditation_submission_updated_at();


INSERT INTO weekly_meditations
  (week_number, start_date, end_date, theme, title, scripture_ref,
   scripture_text, reflection_prompt, further_reading_label,
   further_reading_url)
VALUES
  (1, '2026-06-01', '2026-06-07', 'JESUS', 'Identity in Christ', 'John 15:5',
   'I am the vine; you are the branches. If you remain in me and I in you, you will bear much fruit; apart from me you can do nothing.',
   'Who are you in Jesus? Your worth is not in grades, likes, or people''s opinions — it''s in being His branch.',
   'GotQuestions.org – Identity in Christ',
   'https://www.gotquestions.org/identity-in-Christ.html'),
  (2, '2026-06-08', '2026-06-14', 'JESUS', 'Purpose & Calling', 'Jeremiah 29:11',
   'For I know the plans I have for you, declares the Lord, plans to prosper you and not to harm you, plans to give you hope and a future.',
   'Before your career or degree, God has a calling. Trust His timeline over your own.',
   'DesiringGod.org – God''s Will for Your Life',
   'https://www.desiringgod.org/articles/gods-will-for-your-life'),
  (3, '2026-06-15', '2026-06-21', 'FAMILY', 'Honoring Your Parents', 'Ephesians 6:1-3',
   'Children, obey your parents in the Lord, for this is right. "Honor your father and mother" — which is the first commandment with a promise — "so that it may go well with you and that you may enjoy long life on the earth."',
   'Even when family is hard, God places honor in obedience. Home is a training ground for character.',
   'FocusOnTheFamily.com – Honoring Parents',
   'https://www.focusonthefamily.com/parenting/honoring-parents/'),
  (4, '2026-06-22', '2026-06-28', 'FRIENDS', 'True Friendship', 'Proverbs 27:17',
   'As iron sharpens iron, so one person sharpens another.',
   'Are your friends making you sharper or duller? Choose relationships that push you toward God.',
   'GotQuestions.org – Christian Friendship',
   'https://www.gotquestions.org/Christian-friendship.html'),
  (5, '2026-07-06', '2026-07-12', 'JESUS', 'Seeking God First', 'Matthew 6:33',
   'But seek first his kingdom and his righteousness, and all these things will be given to you as well.',
   'In a season of deadlines, goals, and noise — what comes first in your day?',
   'CrossWalk.com – Seek First the Kingdom',
   'https://www.crosswalk.com/faith/spiritual-life/seek-first-the-kingdom.html'),
  (6, '2026-07-13', '2026-07-19', 'EDUCATION', 'Academic Faithfulness', 'Colossians 3:23',
   'Whatever you do, work at it with all your heart, as working for the Lord, not for human masters.',
   'Your studies are an act of worship. Do them excellently — for an audience of One.',
   'InterVarsity.org – Faith and Academics',
   'https://www.intervarsity.org/blog/faith-and-academics'),
  (7, '2026-07-20', '2026-07-26', 'FRIENDS', 'Peer Pressure & Courage', 'Romans 12:2',
   'Do not conform to the pattern of this world, but be transformed by the renewing of your mind.',
   'The world has a mold. God has a calling. You were made to stand out, not fit in.',
   'FocusOnTheFamily.com – Peer Pressure',
   'https://www.focusonthefamily.com/teens/peer-pressure/'),
  (8, '2026-07-27', '2026-08-02', 'CHURCH', 'Serving the Church', '1 Peter 4:10',
   'Each of you should use whatever gift you have received to serve others, as faithful stewards of God''s grace in its various forms.',
   'You have gifts. The church needs them. Don''t wait until you feel ''ready'' — serve now.',
   'DesiringGod.org – Spiritual Gifts',
   'https://www.desiringgod.org/topics/spiritual-gifts'),
  (9, '2026-08-03', '2026-08-09', 'RELATIONSHIPS', 'Guarding Your Heart', 'Proverbs 4:23',
   'Above all else, guard your heart, for everything you do flows from it.',
   'What you let in shapes who you become. Be intentional about what you watch, hear, and follow.',
   'GotQuestions.org – Guard Your Heart',
   'https://www.gotquestions.org/guard-your-heart.html'),
  (10, '2026-08-10', '2026-08-16', 'FAMILY', 'Family Conflict & Grace', 'Ephesians 4:32',
   'Be kind and compassionate to one another, forgiving each other, just as in Christ God forgave you.',
   'Every home has conflict. The Gospel is the answer. Forgive the way you have been forgiven.',
   'FocusOnTheFamily.com – Conflict at Home',
   'https://www.focusonthefamily.com/family-qa/resolving-conflict-at-home/'),
  (11, '2026-08-17', '2026-08-23', 'JESUS', 'Anxiety & Trust', 'Philippians 4:6-7',
   'Do not be anxious about anything, but in every situation, by prayer and petition, with thanksgiving, present your requests to God. And the peace of God, which transcends all understanding, will guard your hearts and your minds in Christ Jesus.',
   'School stress, family pressure, uncertain futures — bring them all to God in prayer.',
   'DesiringGod.org – Anxiety & Peace',
   'https://www.desiringgod.org/articles/anxiety-is-not-a-sin'),
  (12, '2026-08-24', '2026-08-30', 'CHURCH', 'Unity in the Body', '1 Corinthians 12:27',
   'Now you are the body of Christ, and each one of you is a part of it.',
   'You are not just a church member — you are a living part of Christ''s body. Your presence matters.',
   'GotQuestions.org – Body of Christ',
   'https://www.gotquestions.org/body-of-Christ.html'),
  (13, '2026-08-31', '2026-09-06', 'JESUS', 'New Beginnings', 'Lamentations 3:22-23',
   'Because of the Lord''s great love we are not consumed, for his compassions never fail. They are new every morning; great is your faithfulness.',
   'Each new semester, new chapter — His mercies are new. Start fresh with Him.',
   'CrossWalk.com – New Beginnings',
   'https://www.crosswalk.com/faith/spiritual-life/new-beginnings-with-god.html'),
  (14, '2026-09-07', '2026-09-13', 'EDUCATION', 'Discipline & Growth', 'Proverbs 12:1',
   'Whoever loves discipline loves knowledge, but whoever hates correction is stupid.',
   'Accepting correction is a sign of wisdom, not weakness. Be coachable — in school and in life.',
   'InterVarsity.org – Growing Through Failure',
   'https://www.intervarsity.org/blog/growing-through-failure'),
  (15, '2026-09-14', '2026-09-20', 'FRIENDS', 'The Tongue & Words', 'Proverbs 18:21',
   'The tongue has the power of life and death, and those who love it will eat its fruit.',
   'The way you speak to your friends, family, and classmates carries spiritual weight. Speak life.',
   'GotQuestions.org – Power of Words',
   'https://www.gotquestions.org/power-of-words.html'),
  (16, '2026-09-21', '2026-09-27', 'CHURCH', 'Worship as a Lifestyle', 'Romans 12:1',
   'Therefore, I urge you, brothers and sisters, in view of God''s mercy, to offer your bodies as a living sacrifice, holy and pleasing to God — this is your true and proper worship.',
   'Worship is not just Sunday songs. It''s your Monday choices, your Tuesday conversations, your whole life.',
   'DesiringGod.org – Worship Beyond Singing',
   'https://www.desiringgod.org/articles/worship-is-not-just-singing'),
  (17, '2026-09-28', '2026-10-04', 'EDUCATION', 'Dealing with Failure', 'Psalm 37:24',
   'Though he may stumble, he will not fall, for the Lord upholds him with his hand.',
   'A failed exam or a broken project does not define your future. God holds you up when you stumble.',
   'CrossWalk.com – Failing Well',
   'https://www.crosswalk.com/faith/spiritual-life/failing-well-trusting-god-after-failure.html'),
  (18, '2026-10-05', '2026-10-11', 'RELATIONSHIPS', 'Dating & Purity', '1 Thessalonians 4:3-4',
   'It is God''s will that you should be sanctified: that you should avoid sexual immorality; that each of you should learn to control your own body in a way that is holy and honorable.',
   'God designed romance. His boundaries are not restrictions — they are protection and honor.',
   'FocusOnTheFamily.com – Dating & Purity',
   'https://www.focusonthefamily.com/teens/dating/'),
  (19, '2026-10-12', '2026-10-18', 'JESUS', 'Prayer Life', 'Luke 18:1',
   'Then Jesus told his disciples a parable to show them that they should always pray and not give up.',
   'Prayer is not a last resort — it''s first response. Build a daily habit of conversation with Jesus.',
   'DesiringGod.org – How to Pray',
   'https://www.desiringgod.org/articles/how-to-pray'),
  (20, '2026-10-19', '2026-10-25', 'FAMILY', 'Parental Love & Gaps', 'Psalm 27:10',
   'Though my father and mother forsake me, the Lord will receive me.',
   'Not every home is whole. But God fills every gap. He is the perfect Father you always needed.',
   'GotQuestions.org – The Father Wound',
   'https://www.gotquestions.org/father-wound.html'),
  (21, '2026-10-26', '2026-11-01', 'CHURCH', 'Generosity & Giving', '2 Corinthians 9:7',
   'Each of you should give what you have decided in your heart to give, not reluctantly or under compulsion, for God loves a cheerful giver.',
   'You don''t have to be rich to give. Generosity is a posture of the heart — give your time, talents, presence.',
   'CrossWalk.com – Generosity in the Bible',
   'https://www.crosswalk.com/faith/bible-study/generosity-in-the-bible.html'),
  (22, '2026-11-02', '2026-11-08', 'RELATIONSHIPS', 'Handling Rejection', 'Isaiah 41:10',
   'So do not fear, for I am with you; do not be dismayed, for I am your God. I will strengthen you and help you; I will uphold you with my righteous right hand.',
   'Rejected by a friend, a crush, a school? You are fully accepted by the One who matters most.',
   'GotQuestions.org – Rejection & God''s Acceptance',
   'https://www.gotquestions.org/rejected-by-God.html'),
  (23, '2026-11-09', '2026-11-15', 'JESUS', 'The Word of God', 'Psalm 119:105',
   'Your word is a lamp for my feet, a light on my path.',
   'You can''t see the path without the lamp. Daily Bible reading is your spiritual GPS.',
   'DesiringGod.org – Why Read the Bible',
   'https://www.desiringgod.org/articles/why-read-the-bible'),
  (24, '2026-11-16', '2026-11-22', 'JESUS', 'Thanksgiving & Contentment', '1 Thessalonians 5:16-18',
   'Rejoice always, pray continually, give thanks in all circumstances; for this is God''s will for you in Christ Jesus.',
   'Gratitude is a discipline, not a feeling. Choose thankfulness even in hard seasons.',
   'CrossWalk.com – The Discipline of Gratitude',
   'https://www.crosswalk.com/faith/spiritual-life/the-discipline-of-gratitude.html'),
  (25, '2026-11-23', '2026-11-29', 'FRIENDS', 'Loyalty in Friendship', 'John 15:13',
   'Greater love has no one than this: to lay down one''s life for one''s friends.',
   'Real friendship costs something. Jesus modeled the ultimate loyalty. Be that kind of friend.',
   'GotQuestions.org – Loyalty in Friendship',
   'https://www.gotquestions.org/loyal-friend.html'),
  (26, '2026-11-30', '2026-12-06', 'EDUCATION', 'Finishing Strong', 'Galatians 6:9',
   'Let us not become weary in doing good, for at the proper time we will reap a harvest if we do not give up.',
   'End-of-year burnout is real. But the harvest comes to those who don''t quit. Finish your year well.',
   'InterVarsity.org – Finishing the Semester Well',
   'https://www.intervarsity.org/blog/finishing-well-semester'),
  (27, '2026-12-07', '2026-12-13', 'CHURCH', 'The Church as Family', 'Hebrews 10:24-25',
   'And let us consider how we may spur one another on toward love and good deeds, not giving up meeting together, as some are in the habit of doing, but encouraging one another — and all the more as you see the Day approaching.',
   'Church is not a building or a Sunday habit — it''s your spiritual family. Show up for them.',
   'DesiringGod.org – Why Church Matters',
   'https://www.desiringgod.org/articles/why-church-matters'),
  (28, '2026-12-14', '2026-12-20', 'JESUS', 'The Incarnation & Christmas', 'John 1:14',
   'The Word became flesh and made his dwelling among us. We have seen his glory, the glory of the one and only Son, who came from the Father, full of grace and truth.',
   'Christmas is not about gifts under a tree — it''s the Creator entering creation for you.',
   'GotQuestions.org – The True Meaning of Christmas',
   'https://www.gotquestions.org/meaning-of-Christmas.html'),
  (29, '2026-12-21', '2026-12-27', 'RELATIONSHIPS', 'Love & Relationships', '1 Corinthians 13:4-5',
   'Love is patient, love is kind. It does not envy, it does not boast, it is not proud. It does not dishonor others, it is not self-seeking, it is not easily angered, it keeps no record of wrongs.',
   'This is not just for romance — apply this to friendships, family, and your walk with Jesus.',
   'FocusOnTheFamily.com – What Is Love?',
   'https://www.focusonthefamily.com/marriage/what-is-love/'),
  (30, '2026-12-28', '2027-01-03', 'JESUS', 'Year-End Reflection & New Year', 'Philippians 3:13-14',
   'But one thing I do: Forgetting what is behind and straining toward what is ahead, I press on toward the goal to win the prize for which God has called me heavenward in Christ Jesus.',
   'Let go of regrets, failures, and what-ifs. Press forward. A new year with God starts now.',
   'CrossWalk.com – New Year Devotional',
   'https://www.crosswalk.com/faith/spiritual-life/new-year-devotional.html')
ON CONFLICT (week_number, (EXTRACT(YEAR FROM start_date))) DO NOTHING;
