
ALTER TABLE users
  ADD COLUMN IF NOT EXISTS is_proxy_only BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE users
  ADD COLUMN IF NOT EXISTS created_by_proxy UUID REFERENCES users(id);

ALTER TABLE users
  ADD COLUMN IF NOT EXISTS birthdate DATE;

ALTER TABLE users
  ADD COLUMN IF NOT EXISTS sex TEXT
    CHECK (sex IS NULL OR sex IN ('M', 'F'));

ALTER TABLE users
  ALTER COLUMN email DROP NOT NULL;

CREATE INDEX IF NOT EXISTS idx_users_is_proxy_only
  ON users (is_proxy_only) WHERE is_proxy_only = TRUE;


DROP POLICY IF EXISTS "users_insert_proxy" ON users;
CREATE POLICY "users_insert_proxy" ON users
  FOR INSERT
  WITH CHECK (
    is_proxy_only = TRUE
    AND EXISTS (
      SELECT 1 FROM users self
      WHERE self.id = auth.uid()
        AND self.role IN ('cell_leader','youth_president','pastor','admin')
        AND (
          self.role IN ('youth_president','pastor','admin')
          OR self.group_id = users.group_id
        )
    )
  );


DROP POLICY IF EXISTS "users_update_proxy" ON users;
CREATE POLICY "users_update_proxy" ON users
  FOR UPDATE
  USING (
    is_proxy_only = TRUE
    AND EXISTS (
      SELECT 1 FROM users self
      WHERE self.id = auth.uid()
        AND self.role IN ('cell_leader','youth_president','pastor','admin')
        AND (
          self.role IN ('youth_president','pastor','admin')
          OR self.group_id = users.group_id
        )
    )
  );

DO $$
BEGIN
  PERFORM 1 FROM information_schema.columns
    WHERE table_name = 'users' AND column_name = 'is_proxy_only';
  IF NOT FOUND THEN RAISE EXCEPTION 'is_proxy_only column missing'; END IF;
  PERFORM 1 FROM information_schema.columns
    WHERE table_name = 'users' AND column_name = 'birthdate';
  IF NOT FOUND THEN RAISE EXCEPTION 'birthdate column missing'; END IF;
END $$;
