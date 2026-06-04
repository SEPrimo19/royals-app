-- =============================================================================
-- GRACE — Supabase schema (corrected, ordered, runnable as a single script)
--
-- Differences vs CLAUDE.md (all intentional, required to actually work):
--   1. Table order fixed; users/groups circular FK resolved via ALTER ... ADD.
--   2. Added the missing RLS policies for `users` and `groups` (CLAUDE.md
--      enables RLS on them but defines no policy → total lockout otherwise).
--   3. Enabled Realtime replication on prayers / prayer_intercessions / messages.
--   4. Seed rows at the bottom (one group + today's devotional) for testing.
--
-- Run in: Supabase Dashboard ▸ SQL Editor ▸ paste ▸ Run.
-- =============================================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ---- USERS (no group FK yet — added after groups exists) -------------------
CREATE TABLE users (
  id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  email         TEXT UNIQUE NOT NULL,
  name          TEXT NOT NULL,
  avatar_url    TEXT,
  role          TEXT NOT NULL DEFAULT 'member',
  group_id      UUID,
  fcm_token     TEXT,
  streak        INTEGER DEFAULT 0,
  last_devo_at  TIMESTAMPTZ,
  created_at    TIMESTAMPTZ DEFAULT NOW()
);

-- ---- GROUPS ----------------------------------------------------------------
CREATE TABLE groups (
  id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  name        TEXT NOT NULL,
  leader_id   UUID REFERENCES users(id),
  description TEXT,
  created_at  TIMESTAMPTZ DEFAULT NOW()
);

-- Resolve the circular reference now that both tables exist.
ALTER TABLE users
  ADD CONSTRAINT users_group_id_fkey
  FOREIGN KEY (group_id) REFERENCES groups(id);

CREATE TABLE group_members (
  user_id    UUID REFERENCES users(id) ON DELETE CASCADE,
  group_id   UUID REFERENCES groups(id) ON DELETE CASCADE,
  joined_at  TIMESTAMPTZ DEFAULT NOW(),
  PRIMARY KEY (user_id, group_id)
);

-- ---- READING PLANS / DEVOTIONALS ------------------------------------------
CREATE TABLE reading_plans (
  id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  title         TEXT NOT NULL,
  description   TEXT,
  duration_days INTEGER NOT NULL,
  created_by    UUID REFERENCES users(id)
);

CREATE TABLE devotionals (
  id             UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  scheduled_date DATE UNIQUE NOT NULL,
  title          TEXT NOT NULL,
  verse_ref      TEXT NOT NULL,
  verse_text     TEXT NOT NULL,
  reflection     TEXT NOT NULL,
  prayer_starter TEXT NOT NULL,
  journal_prompt TEXT NOT NULL,
  plan_id        UUID REFERENCES reading_plans(id),
  created_by     UUID REFERENCES users(id),
  created_at     TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE user_devo_progress (
  user_id        UUID REFERENCES users(id) ON DELETE CASCADE,
  devo_id        UUID REFERENCES devotionals(id) ON DELETE CASCADE,
  completed_at   TIMESTAMPTZ DEFAULT NOW(),
  journal_entry  TEXT,
  PRIMARY KEY (user_id, devo_id)
);

-- ---- PRAYERS ---------------------------------------------------------------
CREATE TABLE prayers (
  id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id       UUID REFERENCES users(id) ON DELETE CASCADE,
  content       TEXT NOT NULL,
  is_anonymous  BOOLEAN DEFAULT FALSE,
  category      TEXT NOT NULL,
  status        TEXT DEFAULT 'active',
  pray_count    INTEGER DEFAULT 0,
  is_flagged    BOOLEAN DEFAULT FALSE,
  expires_at    TIMESTAMPTZ DEFAULT (NOW() + INTERVAL '30 days'),
  created_at    TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE prayer_intercessions (
  prayer_id  UUID REFERENCES prayers(id) ON DELETE CASCADE,
  user_id    UUID REFERENCES users(id) ON DELETE CASCADE,
  prayed_at  TIMESTAMPTZ DEFAULT NOW(),
  PRIMARY KEY (prayer_id, user_id)
);

-- ---- FEED ------------------------------------------------------------------
CREATE TABLE posts (
  id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id         UUID REFERENCES users(id) ON DELETE CASCADE,
  type            TEXT NOT NULL,
  content         TEXT NOT NULL,
  image_url       TEXT,
  verse_ref       TEXT,
  is_highlighted  BOOLEAN DEFAULT FALSE,
  highlighted_by  UUID REFERENCES users(id),
  is_flagged      BOOLEAN DEFAULT FALSE,
  created_at      TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE reactions (
  post_id        UUID REFERENCES posts(id) ON DELETE CASCADE,
  user_id        UUID REFERENCES users(id) ON DELETE CASCADE,
  reaction_type  TEXT NOT NULL,
  created_at     TIMESTAMPTZ DEFAULT NOW(),
  PRIMARY KEY (post_id, user_id)
);

CREATE TABLE comments (
  id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  post_id     UUID REFERENCES posts(id) ON DELETE CASCADE,
  user_id     UUID REFERENCES users(id) ON DELETE CASCADE,
  content     TEXT NOT NULL,
  parent_id   UUID REFERENCES comments(id),
  created_at  TIMESTAMPTZ DEFAULT NOW()
);

-- ---- MESSAGES / CHECK-INS / EVENTS ----------------------------------------
CREATE TABLE messages (
  id           UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  sender_id    UUID REFERENCES users(id) ON DELETE CASCADE,
  receiver_id  UUID REFERENCES users(id) ON DELETE CASCADE,
  content      TEXT NOT NULL,
  is_read      BOOLEAN DEFAULT FALSE,
  sent_at      TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE checkins (
  id           UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id      UUID REFERENCES users(id) ON DELETE CASCADE,
  leader_id    UUID REFERENCES users(id),
  answers      JSONB NOT NULL,
  submitted_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE events (
  id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  title         TEXT NOT NULL,
  description   TEXT,
  event_date    TIMESTAMPTZ NOT NULL,
  location      TEXT,
  created_by    UUID REFERENCES users(id),
  is_recurring  BOOLEAN DEFAULT FALSE,
  recur_rule    TEXT,
  created_at    TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE event_rsvp (
  event_id      UUID REFERENCES events(id) ON DELETE CASCADE,
  user_id       UUID REFERENCES users(id) ON DELETE CASCADE,
  status        TEXT DEFAULT 'going',
  responded_at  TIMESTAMPTZ DEFAULT NOW(),
  PRIMARY KEY (event_id, user_id)
);

CREATE TABLE mood_checkins (
  id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id     UUID REFERENCES users(id) ON DELETE CASCADE,
  mood_score  INTEGER NOT NULL CHECK (mood_score BETWEEN 1 AND 5),
  note        TEXT,
  created_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE notifications_log (
  id           UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  sent_by      UUID REFERENCES users(id),
  target_type  TEXT NOT NULL,
  target_id    UUID,
  title        TEXT NOT NULL,
  body         TEXT NOT NULL,
  sent_at      TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE challenges (
  id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  title       TEXT NOT NULL,
  description TEXT,
  group_id    UUID REFERENCES groups(id) ON DELETE CASCADE,
  created_by  UUID REFERENCES users(id),
  start_date  DATE NOT NULL,
  end_date    DATE NOT NULL
);

CREATE TABLE challenge_progress (
  challenge_id      UUID REFERENCES challenges(id) ON DELETE CASCADE,
  user_id           UUID REFERENCES users(id) ON DELETE CASCADE,
  checked_in_dates  JSONB DEFAULT '[]',
  PRIMARY KEY (challenge_id, user_id)
);

-- =============================================================================
-- ROW LEVEL SECURITY
-- =============================================================================
ALTER TABLE users                ENABLE ROW LEVEL SECURITY;
ALTER TABLE groups               ENABLE ROW LEVEL SECURITY;
ALTER TABLE prayers              ENABLE ROW LEVEL SECURITY;
ALTER TABLE prayer_intercessions ENABLE ROW LEVEL SECURITY;
ALTER TABLE posts                ENABLE ROW LEVEL SECURITY;
ALTER TABLE reactions            ENABLE ROW LEVEL SECURITY;
ALTER TABLE comments             ENABLE ROW LEVEL SECURITY;
ALTER TABLE messages             ENABLE ROW LEVEL SECURITY;
ALTER TABLE checkins             ENABLE ROW LEVEL SECURITY;
ALTER TABLE mood_checkins        ENABLE ROW LEVEL SECURITY;
ALTER TABLE devotionals          ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_devo_progress   ENABLE ROW LEVEL SECURITY;

-- USERS — added (missing in CLAUDE.md, required for sign-up/profile to work).
CREATE POLICY "users_select" ON users
  FOR SELECT USING (auth.role() = 'authenticated');
CREATE POLICY "users_insert" ON users
  FOR INSERT WITH CHECK (auth.uid() = id);
CREATE POLICY "users_update" ON users
  FOR UPDATE USING (auth.uid() = id);

-- GROUPS — added (ProfileSetup reads the group list).
CREATE POLICY "groups_select" ON groups
  FOR SELECT USING (auth.role() = 'authenticated');

-- PRAYERS
CREATE POLICY "prayers_select" ON prayers
  FOR SELECT USING (auth.role() = 'authenticated');
CREATE POLICY "prayers_insert" ON prayers
  FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "prayers_update" ON prayers
  FOR UPDATE USING (auth.uid() = user_id);

-- PRAYER INTERCESSIONS (needed for the Realtime pray-count feature)
CREATE POLICY "intercession_select" ON prayer_intercessions
  FOR SELECT USING (auth.role() = 'authenticated');
CREATE POLICY "intercession_insert" ON prayer_intercessions
  FOR INSERT WITH CHECK (auth.uid() = user_id);

-- MESSAGES — only sender and receiver
CREATE POLICY "messages_select" ON messages
  FOR SELECT USING (auth.uid() = sender_id OR auth.uid() = receiver_id);
CREATE POLICY "messages_insert" ON messages
  FOR INSERT WITH CHECK (auth.uid() = sender_id);

-- MOOD CHECK-INS
CREATE POLICY "mood_own" ON mood_checkins
  FOR SELECT USING (auth.uid() = user_id);

-- DEVOTIONALS
CREATE POLICY "devo_select" ON devotionals
  FOR SELECT USING (auth.role() = 'authenticated');
CREATE POLICY "devo_insert" ON devotionals
  FOR INSERT WITH CHECK (
    EXISTS (SELECT 1 FROM users WHERE id = auth.uid()
            AND role IN ('pastor','admin','youth_president'))
  );

-- JOURNAL — fully private to the owner
CREATE POLICY "journal_select" ON user_devo_progress
  FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "journal_insert" ON user_devo_progress
  FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "journal_update" ON user_devo_progress
  FOR UPDATE USING (auth.uid() = user_id);

-- =============================================================================
-- REALTIME (required for Prompt 5 live pray-count + Prompt 6 chat)
-- =============================================================================
ALTER PUBLICATION supabase_realtime ADD TABLE prayers;
ALTER PUBLICATION supabase_realtime ADD TABLE prayer_intercessions;
ALTER PUBLICATION supabase_realtime ADD TABLE messages;

-- =============================================================================
-- SEED DATA (so the app shows something immediately for testing)
-- =============================================================================
INSERT INTO groups (name, description)
VALUES ('GRACE Youth (Default)', 'Default cell group for testing');

INSERT INTO devotionals
  (scheduled_date, title, verse_ref, verse_text, reflection, prayer_starter, journal_prompt)
VALUES (
  CURRENT_DATE,
  'Anchored in Hope',
  'Jeremiah 29:11',
  'For I know the plans I have for you, declares the Lord, plans to prosper you and not to harm you, plans to give you hope and a future.',
  'God''s plans for you are good, even when the path is unclear. Today, rest in the truth that your future is held by a faithful God.',
  'Lord, thank You that Your plans for me are good. Help me trust You with what I cannot yet see...',
  'Where do you need to trust God''s plan over your own right now?'
);
