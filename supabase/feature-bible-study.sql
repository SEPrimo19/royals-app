
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS bible_notes (
  id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  title       TEXT,
  book_order  INTEGER,
  chapter     INTEGER,
  content     TEXT NOT NULL DEFAULT '',
  created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_bible_notes_chapter
  ON bible_notes (user_id, book_order, chapter)
  WHERE book_order IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_bible_notes_user
  ON bible_notes (user_id, updated_at DESC);

ALTER TABLE bible_notes ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS bible_notes_owner ON bible_notes;
CREATE POLICY bible_notes_owner ON bible_notes
  FOR ALL USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

CREATE TABLE IF NOT EXISTS bible_highlights (
  user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  book_order  INTEGER NOT NULL,
  chapter     INTEGER NOT NULL,
  verse       INTEGER NOT NULL,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  PRIMARY KEY (user_id, book_order, chapter, verse)
);

ALTER TABLE bible_highlights ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS bible_highlights_owner ON bible_highlights;
CREATE POLICY bible_highlights_owner ON bible_highlights
  FOR ALL USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);
