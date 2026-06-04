-- =============================================================================
-- GRACE — Security Phase B: account deletion cascade (defensive v2)
--
-- The app's "Delete my account" button invokes the `delete-account` Edge
-- Function. The function deletes the public.users row, which today would
-- fail with FK violations on a long list of "created_by" / "leader_id" /
-- "*_by_proxy" / "highlighted_by" / "sent_by" columns that default to
-- NO ACTION (block-on-delete).
--
-- This migration flips every "soft" user FK to ON DELETE SET NULL — the
-- data SURVIVES (content stays in place; audit log keeps the record) but
-- the attribution gets nulled out, so deleting a leader doesn't blast
-- away the church's history of devotionals, posts they highlighted, etc.
--
-- The "hard" user FKs (user_id on personal data like prayers, posts,
-- journal, messages, game_attempts, etc.) already have ON DELETE CASCADE
-- — verified during the audit. Those stay as-is; deleting a user wipes
-- their own personal records.
--
-- DEFENSIVE — wrapped in a helper that skips silently if the column or
-- table doesn't exist in your deployed schema (some tables from the
-- original spec were never actually created). Each helper call emits a
-- NOTICE for inspection in the Logs / Messages pane.
--
-- Safe to re-run.
-- =============================================================================

-- Helper: if (table_name).(column_name) exists, drop the named FK
-- constraint (if present) and re-add it with ON DELETE SET NULL.
-- Silently skips missing tables/columns/constraints.
CREATE OR REPLACE FUNCTION set_user_fk_null(
  p_table TEXT,
  p_column TEXT,
  p_constraint TEXT
)
RETURNS VOID
LANGUAGE plpgsql
AS $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.tables
     WHERE table_schema = 'public' AND table_name = p_table
  ) THEN
    RAISE NOTICE 'set_user_fk_null: table % does not exist — skip', p_table;
    RETURN;
  END IF;
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
     WHERE table_schema = 'public'
       AND table_name = p_table
       AND column_name = p_column
  ) THEN
    RAISE NOTICE
      'set_user_fk_null: column %.% does not exist — skip',
      p_table, p_column;
    RETURN;
  END IF;
  EXECUTE format(
    'ALTER TABLE %I DROP CONSTRAINT IF EXISTS %I',
    p_table, p_constraint
  );
  EXECUTE format(
    'ALTER TABLE %I ADD CONSTRAINT %I '
    || 'FOREIGN KEY (%I) REFERENCES users(id) ON DELETE SET NULL',
    p_table, p_constraint, p_column
  );
  RAISE NOTICE 'set_user_fk_null: ok %.%', p_table, p_column;
END;
$$;

-- Each call below is independent. A failure in one no longer aborts the
-- whole migration — the helper either applies the change or skips with a
-- NOTICE.
SELECT set_user_fk_null('groups',                 'leader_id',          'groups_leader_id_fkey');
SELECT set_user_fk_null('devotionals',            'created_by',         'devotionals_created_by_fkey');
SELECT set_user_fk_null('reading_plans',          'created_by',         'reading_plans_created_by_fkey');
SELECT set_user_fk_null('events',                 'created_by',         'events_created_by_fkey');
SELECT set_user_fk_null('posts',                  'highlighted_by',     'posts_highlighted_by_fkey');
SELECT set_user_fk_null('checkins',               'leader_id',          'checkins_leader_id_fkey');
SELECT set_user_fk_null('notifications_log',      'sent_by',            'notifications_log_sent_by_fkey');
SELECT set_user_fk_null('challenges',             'created_by',         'challenges_created_by_fkey');
SELECT set_user_fk_null('bible_questions',        'created_by',         'bible_questions_created_by_fkey');
SELECT set_user_fk_null('bible_passages',         'created_by',         'bible_passages_created_by_fkey');
SELECT set_user_fk_null('bible_characters',       'created_by',         'bible_characters_created_by_fkey');
SELECT set_user_fk_null('memory_card_pairs',      'created_by',         'memory_card_pairs_created_by_fkey');
SELECT set_user_fk_null('bible_verse_scrambles',  'created_by',         'bible_verse_scrambles_created_by_fkey');
SELECT set_user_fk_null('bible_events',           'created_by',         'bible_events_created_by_fkey');
SELECT set_user_fk_null('users',                  'claimed_by_user_id', 'users_claimed_by_user_id_fkey');
SELECT set_user_fk_null('group_members',          'created_by_proxy',   'group_members_created_by_proxy_fkey');
SELECT set_user_fk_null('prayers',                'posted_by_proxy',    'prayers_posted_by_proxy_fkey');

-- Drop the helper after use — single-purpose, no reason to leave around.
DROP FUNCTION IF EXISTS set_user_fk_null(TEXT, TEXT, TEXT);

-- Sanity check after running (manual):
--   SELECT conrelid::regclass::text AS table_name,
--          conname,
--          CASE confdeltype
--            WHEN 'a' THEN 'NO ACTION'
--            WHEN 'r' THEN 'RESTRICT'
--            WHEN 'c' THEN 'CASCADE'
--            WHEN 'n' THEN 'SET NULL'
--            WHEN 'd' THEN 'SET DEFAULT'
--          END AS on_delete
--     FROM pg_constraint
--    WHERE contype = 'f'
--      AND conname LIKE '%\_fkey' ESCAPE '\'
--    ORDER BY 1, 2;
-- The constraints listed above should now show SET NULL.
