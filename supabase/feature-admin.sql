
ALTER TABLE users ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "users_select"        ON users;
DROP POLICY IF EXISTS "users_update"        ON users;
DROP POLICY IF EXISTS "users_update_self"   ON users;
DROP POLICY IF EXISTS "users_update_admin"  ON users;

CREATE POLICY "users_select" ON users
  FOR SELECT USING (auth.role() = 'authenticated');

CREATE POLICY "users_update" ON users
  FOR UPDATE USING (
    auth.uid() = id
    OR EXISTS (
      SELECT 1 FROM users u
      WHERE u.id = auth.uid() AND u.role IN ('youth_president','admin')
    )
  );

CREATE TABLE IF NOT EXISTS audit_log (
  id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  actor_id    UUID REFERENCES users(id) ON DELETE SET NULL,
  target_id   UUID REFERENCES users(id) ON DELETE SET NULL,
  action      TEXT NOT NULL,
  old_value   TEXT,
  new_value   TEXT,
  metadata    JSONB,
  created_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_audit_log_created_at ON audit_log (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_log_target     ON audit_log (target_id);

ALTER TABLE audit_log ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "audit_log_select" ON audit_log;
CREATE POLICY "audit_log_select" ON audit_log
  FOR SELECT USING (
    EXISTS (
      SELECT 1 FROM users u
      WHERE u.id = auth.uid() AND u.role IN ('youth_president','admin')
    )
  );


CREATE OR REPLACE FUNCTION log_role_change() RETURNS TRIGGER AS $$
BEGIN
  IF OLD.role IS DISTINCT FROM NEW.role THEN
    INSERT INTO audit_log (actor_id, target_id, action, old_value, new_value)
    VALUES (auth.uid(), NEW.id, 'role_change', OLD.role, NEW.role);
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS trg_log_role_change ON users;
CREATE TRIGGER trg_log_role_change
  AFTER UPDATE OF role ON users
  FOR EACH ROW EXECUTE FUNCTION log_role_change();
