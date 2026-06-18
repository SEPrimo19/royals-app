
INSERT INTO app_versions (version_code, version_name, download_url, release_notes, is_mandatory)
VALUES (
  4,
  '1.3.0',
  'https://drive.google.com/file/d/<DRIVE-FILE-ID>/view',
  '📖 New: full offline Bible (KJV) — read any chapter (menu → Bible, or the ' ||
  '"Read the Word" card on Home), search the whole Bible, and add a verse to a ' ||
  'Feed post with the verse picker. ' ||
  '📝 New: Study Mode — highlight verses and take notes side-by-side with the ' ||
  'text, keep sermon / Bible-study notes, and pull verses straight into them. ' ||
  '🖼 New: turn a verse (or a whole passage) into a shareable image with ' ||
  'background designs, your own photo, and font styles. ' ||
  '🏆 New: Team leaderboard — see which cell group is on top this month. ' ||
  '🤝 New: Council members can join a cell and lead their own. ' ||
  '☁ Improved: your devotional reflections + journal now back up to your ' ||
  'account, so they''re restored when you reinstall or switch phones. ' ||
  '✓ Fixed: Bible Games answer flicker, a stray refresh circle on Home, a ' ||
  'duplicate offline banner, and daily challenges now reset at midnight.',
  FALSE
)
ON CONFLICT (version_code) DO UPDATE SET
  version_name  = EXCLUDED.version_name,
  download_url  = EXCLUDED.download_url,
  release_notes = EXCLUDED.release_notes,
  is_mandatory  = EXCLUDED.is_mandatory,
  released_at   = NOW();
