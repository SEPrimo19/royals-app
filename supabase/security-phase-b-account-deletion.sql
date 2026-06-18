
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

DROP FUNCTION IF EXISTS set_user_fk_null(TEXT, TEXT, TEXT);

