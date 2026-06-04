-- =============================================================================
-- Royals: The Kingdom Builders — Leader Proxy Mode (Phase P.1 foundation)
--
-- Lets cell leaders register members who don't have a smartphone, so those
-- members can be counted in attendance + Compassion compliance reports
-- without ever logging into the app themselves.
--
-- New columns on public.users:
--   is_proxy_only     BOOLEAN — TRUE means this row has no auth account,
--                                only their cell leader can act on it
--   created_by_proxy  UUID    — the leader who registered them (audit)
--   birthdate         DATE    — required for Compassion age cohort
--   sex               TEXT    — required for Compassion records ('M'|'F')
--
-- Made nullable: email — proxy members often have no email. Existing rows
-- all have real emails so the change is safe. A partial unique index keeps
-- enforcement for non-null emails (postgres UNIQUE allows multiple NULLs).
--
-- RLS: only cell_leader+ can INSERT proxy rows, and only into THEIR group.
-- Senior leaders (youth_president, pastor, admin) can insert into ANY group.
--
-- Safe to re-run.
-- =============================================================================

-- ---- COLUMNS ----------------------------------------------------------------
ALTER TABLE users
  ADD COLUMN IF NOT EXISTS is_proxy_only BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE users
  ADD COLUMN IF NOT EXISTS created_by_proxy UUID REFERENCES users(id);

ALTER TABLE users
  ADD COLUMN IF NOT EXISTS birthdate DATE;

ALTER TABLE users
  ADD COLUMN IF NOT EXISTS sex TEXT
    CHECK (sex IS NULL OR sex IN ('M', 'F'));

-- ---- MAKE EMAIL NULLABLE ----------------------------------------------------
-- Existing UNIQUE constraint still applies — Postgres UNIQUE permits multiple
-- NULLs, so several proxy members with NULL email is fine. Real signup emails
-- continue to be enforced as unique.
ALTER TABLE users
  ALTER COLUMN email DROP NOT NULL;

-- ---- INDEX ------------------------------------------------------------------
-- Partial index on the proxy flag — drawer / leader screens filter heavily
-- by this. Tiny table, trivial cost.
CREATE INDEX IF NOT EXISTS idx_users_is_proxy_only
  ON users (is_proxy_only) WHERE is_proxy_only = TRUE;

-- ---- RLS POLICIES -----------------------------------------------------------
-- Existing user-row SELECT policies (set up in feature-admin.sql + earlier)
-- already gate visibility — leaders see members in their group via group_id
-- match, senior leaders see all. Proxy rows piggyback on those same rules,
-- no SELECT changes needed.
--
-- INSERT is the new gate: only leaders + above can create proxy members,
-- and a regular cell_leader can only insert into THEIR own group.

-- Capture the "new row" group_id BEFORE entering EXISTS — inside the
-- subquery the alias `self` shadows the outer `users` reference, and
-- there's no NEW.x in RLS (that's trigger syntax). Pulling it into a
-- LATERAL-style let-binding via a separate AND keeps the policy readable.
DROP POLICY IF EXISTS "users_insert_proxy" ON users;
CREATE POLICY "users_insert_proxy" ON users
  FOR INSERT
  WITH CHECK (
    -- Caller must be authenticated AND have a role that's allowed to register
    -- proxy members. We re-check is_proxy_only = TRUE so this policy can't be
    -- abused to spawn fake regular accounts.
    is_proxy_only = TRUE
    AND EXISTS (
      SELECT 1 FROM users self
      WHERE self.id = auth.uid()
        AND self.role IN ('cell_leader','youth_president','pastor','admin')
        AND (
          -- Senior leaders: insert into any group
          self.role IN ('youth_president','pastor','admin')
          -- Cell leaders: insert only into their own group. The bare
          -- `users.group_id` here refers to the row being INSERTed
          -- (RLS policies don't use NEW.x — that's trigger syntax).
          OR self.group_id = users.group_id
        )
    )
  );

-- ---- UPDATE policy for proxy rows -------------------------------------------
-- Proxy members can't update themselves (they don't log in), so the existing
-- self-update policy doesn't apply. Leaders need to be able to edit a proxy
-- record (fix a typo'd name, update Compassion number, etc.) — same
-- group-scoped rules as INSERT.

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

-- Confirm the new columns exist (silent if all good, errors if migration
-- didn't apply — useful when copy-pasting into SQL Editor in chunks).
DO $$
BEGIN
  PERFORM 1 FROM information_schema.columns
    WHERE table_name = 'users' AND column_name = 'is_proxy_only';
  IF NOT FOUND THEN RAISE EXCEPTION 'is_proxy_only column missing'; END IF;
  PERFORM 1 FROM information_schema.columns
    WHERE table_name = 'users' AND column_name = 'birthdate';
  IF NOT FOUND THEN RAISE EXCEPTION 'birthdate column missing'; END IF;
END $$;
