
DO $$
BEGIN
  IF to_regclass('public.events') IS NOT NULL THEN
    DROP POLICY IF EXISTS "events_insert" ON events;
    CREATE POLICY "events_insert" ON events
      FOR INSERT WITH CHECK (
        EXISTS (
          SELECT 1 FROM users
          WHERE id = auth.uid()
            AND role IN ('cell_leader','council','youth_president','pastor','admin')
        )
      );
  END IF;
END $$;

DO $$
BEGIN
  IF to_regclass('public.posts') IS NOT NULL THEN
    DROP POLICY IF EXISTS "posts_update_leader" ON posts;
    CREATE POLICY "posts_update_leader" ON posts
      FOR UPDATE USING (
        EXISTS (
          SELECT 1 FROM users
          WHERE id = auth.uid()
            AND role IN ('cell_leader','council','youth_president','pastor','admin')
        )
      );
  END IF;
END $$;


DO $$
BEGIN
  IF to_regclass('public.bible_questions') IS NOT NULL THEN
    DROP POLICY IF EXISTS "bq_write" ON bible_questions;
    CREATE POLICY "bq_write" ON bible_questions
      FOR ALL USING (
        EXISTS (
          SELECT 1 FROM users
          WHERE id = auth.uid()
            AND role IN ('cell_leader','council','youth_president','pastor','admin')
        )
      );
  END IF;
END $$;

DO $$
BEGIN
  IF to_regclass('public.bible_passages') IS NOT NULL THEN
    DROP POLICY IF EXISTS "bp_write" ON bible_passages;
    CREATE POLICY "bp_write" ON bible_passages
      FOR ALL USING (
        EXISTS (
          SELECT 1 FROM users
          WHERE id = auth.uid()
            AND role IN ('cell_leader','council','youth_president','pastor','admin')
        )
      );
  END IF;
END $$;

DO $$
DECLARE
  tbl TEXT;
BEGIN
  FOR tbl IN SELECT unnest(ARRAY[
    'bible_characters',
    'memory_card_pairs',
    'bible_verse_scrambles',
    'bible_events'
  ]) LOOP
    IF to_regclass('public.' || tbl) IS NULL THEN
      CONTINUE;  -- skip missing tables silently
    END IF;
    EXECUTE format($f$
      DROP POLICY IF EXISTS "%1$s_insert" ON %1$I;
      CREATE POLICY "%1$s_insert" ON %1$I
        FOR INSERT WITH CHECK (
          EXISTS (
            SELECT 1 FROM users u
            WHERE u.id = auth.uid()
              AND u.role IN ('cell_leader','council','youth_president','pastor','admin')
          )
        );
      DROP POLICY IF EXISTS "%1$s_update" ON %1$I;
      CREATE POLICY "%1$s_update" ON %1$I
        FOR UPDATE USING (
          EXISTS (
            SELECT 1 FROM users u
            WHERE u.id = auth.uid()
              AND u.role IN ('cell_leader','council','youth_president','pastor','admin')
          )
        );
      DROP POLICY IF EXISTS "%1$s_delete" ON %1$I;
      CREATE POLICY "%1$s_delete" ON %1$I
        FOR DELETE USING (
          EXISTS (
            SELECT 1 FROM users u
            WHERE u.id = auth.uid()
              AND u.role IN ('cell_leader','council','youth_president','pastor','admin')
          )
        );
    $f$, tbl);
  END LOOP;
END $$;

DO $$
BEGIN
  IF to_regclass('public.checkins') IS NOT NULL THEN
    DROP POLICY IF EXISTS "checkins_select_leader" ON checkins;
    CREATE POLICY "checkins_select_leader" ON checkins
      FOR SELECT USING (
        auth.uid() = user_id OR EXISTS (
          SELECT 1 FROM users
          WHERE id = auth.uid()
            AND role IN ('cell_leader','council','youth_president','pastor','admin')
        )
      );
  END IF;
END $$;

