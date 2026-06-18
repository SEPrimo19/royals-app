
DROP POLICY IF EXISTS "groups_delete" ON groups;

CREATE POLICY "groups_delete" ON groups
  FOR DELETE USING (
    auth.uid() = leader_id
    OR EXISTS (
      SELECT 1 FROM users WHERE id = auth.uid()
        AND role IN ('youth_president','pastor','admin')
    )
  );


DO $$
BEGIN
  -- Drop the existing constraint if present, then add it back with the
  -- new ON DELETE clause. Constraint name follows the default Postgres
  -- naming pattern used by schema.sql:
  --   ALTER TABLE users ADD CONSTRAINT users_group_id_fkey ...
  IF EXISTS (
    SELECT 1 FROM pg_constraint
     WHERE conname = 'users_group_id_fkey'
       AND conrelid = 'public.users'::regclass
  ) THEN
    ALTER TABLE users DROP CONSTRAINT users_group_id_fkey;
  END IF;
END $$;

ALTER TABLE users
  ADD CONSTRAINT users_group_id_fkey
  FOREIGN KEY (group_id) REFERENCES groups(id)
  ON DELETE SET NULL;

