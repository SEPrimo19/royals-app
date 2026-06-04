-- =============================================================================
-- GRACE — Security Phase B (PATCH): catch the three proxy-attribution FKs
-- the original migration didn't know about.
--
-- After running the first Phase B SQL, the sanity-check query showed these
-- three were still NO ACTION:
--   event_attendance.posted_by_proxy
--   users.created_by_proxy
--   user_meditation_submissions.submitted_by_proxy
--
-- They were added by Leader Proxy Mode features that hadn't shipped when I
-- wrote the initial migration. Same fix — flip to ON DELETE SET NULL so
-- deleting a leader doesn't FK-violate on their proxy actions; the proxy
-- records survive but the attribution gets nulled.
--
-- Reuses the same defensive helper pattern.
-- Safe to re-run.
-- =============================================================================

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

SELECT set_user_fk_null(
  'event_attendance',
  'posted_by_proxy',
  'event_attendance_posted_by_proxy_fkey'
);
SELECT set_user_fk_null(
  'users',
  'created_by_proxy',
  'users_created_by_proxy_fkey'
);
SELECT set_user_fk_null(
  'user_meditation_submissions',
  'submitted_by_proxy',
  'user_meditation_submissions_submitted_by_proxy_fkey'
);

DROP FUNCTION IF EXISTS set_user_fk_null(TEXT, TEXT, TEXT);
