
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS app_versions (
  version_code   INTEGER PRIMARY KEY,
  version_name   TEXT NOT NULL,
  download_url   TEXT NOT NULL,
  release_notes  TEXT,
  is_mandatory   BOOLEAN NOT NULL DEFAULT FALSE,
  released_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  released_by    UUID REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_app_versions_code_desc
  ON app_versions (version_code DESC);


ALTER TABLE app_versions ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS app_versions_select ON app_versions;
DROP POLICY IF EXISTS app_versions_write_admin ON app_versions;

CREATE POLICY app_versions_select ON app_versions
  FOR SELECT USING (auth.role() = 'authenticated');

CREATE POLICY app_versions_write_admin ON app_versions
  FOR ALL USING (
    EXISTS (
      SELECT 1 FROM users
      WHERE id = auth.uid()
        AND role IN ('youth_president','pastor','admin')
    )
  ) WITH CHECK (
    EXISTS (
      SELECT 1 FROM users
      WHERE id = auth.uid()
        AND role IN ('youth_president','pastor','admin')
    )
  );

