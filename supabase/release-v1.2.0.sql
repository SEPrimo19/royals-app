
INSERT INTO app_versions (version_code, version_name, download_url, release_notes, is_mandatory)
VALUES (
  3,
  '1.2.0',
  'https://drive.google.com/file/d/<DRIVE-FILE-ID>/view',
  'New: tap the Discipleship card to open the library · pull-to-refresh on Home · ' ||
  'offline & back-online indicators · 130+ new Bible Games questions across all modes. ' ||
  'Fixed: Today''s Challenge now always shows an activity (Pick another cycles forever) · ' ||
  'Done-today markings in the Discipleship library · note hearts reset when you post a new note.',
  FALSE
)
ON CONFLICT (version_code) DO UPDATE SET
  version_name  = EXCLUDED.version_name,
  download_url  = EXCLUDED.download_url,
  release_notes = EXCLUDED.release_notes,
  is_mandatory  = EXCLUDED.is_mandatory,
  released_at   = NOW();
