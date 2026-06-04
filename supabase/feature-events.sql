-- =============================================================================
-- GRACE — Feature: Events + RSVP
--
-- The `events` and `event_rsvp` tables already exist (created by schema.sql)
-- but had no RLS policies. This enables RLS, adds policies, and seeds a few
-- upcoming events for testing.
--
-- Safe to re-run. Run in: Supabase Dashboard → SQL Editor → paste → Run.
-- =============================================================================

ALTER TABLE events     ENABLE ROW LEVEL SECURITY;
ALTER TABLE event_rsvp ENABLE ROW LEVEL SECURITY;

-- ---- EVENTS ----------------------------------------------------------------
DROP POLICY IF EXISTS "events_select" ON events;
DROP POLICY IF EXISTS "events_insert" ON events;
DROP POLICY IF EXISTS "events_update" ON events;
DROP POLICY IF EXISTS "events_delete" ON events;

-- Everyone signed in can see events.
CREATE POLICY "events_select" ON events
  FOR SELECT USING (auth.role() = 'authenticated');

-- Only leaders create events.
CREATE POLICY "events_insert" ON events
  FOR INSERT WITH CHECK (
    EXISTS (
      SELECT 1 FROM users WHERE id = auth.uid()
        AND role IN ('cell_leader','youth_president','pastor','admin')
    )
  );

-- The creator (or senior leaders) can edit/delete.
CREATE POLICY "events_update" ON events
  FOR UPDATE USING (
    auth.uid() = created_by
    OR EXISTS (
      SELECT 1 FROM users WHERE id = auth.uid()
        AND role IN ('youth_president','pastor','admin')
    )
  );
CREATE POLICY "events_delete" ON events
  FOR DELETE USING (
    auth.uid() = created_by
    OR EXISTS (
      SELECT 1 FROM users WHERE id = auth.uid()
        AND role IN ('youth_president','pastor','admin')
    )
  );

-- ---- EVENT RSVP ------------------------------------------------------------
DROP POLICY IF EXISTS "rsvp_select" ON event_rsvp;
DROP POLICY IF EXISTS "rsvp_insert" ON event_rsvp;
DROP POLICY IF EXISTS "rsvp_update" ON event_rsvp;
DROP POLICY IF EXISTS "rsvp_delete" ON event_rsvp;

-- RSVP counts are public to signed-in users (so "12 going" can be shown).
CREATE POLICY "rsvp_select" ON event_rsvp
  FOR SELECT USING (auth.role() = 'authenticated');

-- A user manages only their own RSVP row.
CREATE POLICY "rsvp_insert" ON event_rsvp
  FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "rsvp_update" ON event_rsvp
  FOR UPDATE USING (auth.uid() = user_id);
CREATE POLICY "rsvp_delete" ON event_rsvp
  FOR DELETE USING (auth.uid() = user_id);

-- ---- SEED (only if no upcoming events exist) -------------------------------
INSERT INTO events (title, description, event_date, location)
SELECT * FROM (VALUES
  ('Friday Youth Night',
   'Worship, the Word, and fellowship. Bring a friend!',
   NOW() + INTERVAL '3 days', 'Main Sanctuary'),
  ('Community Outreach',
   'Serving our neighborhood together — food, prayer, and presence.',
   NOW() + INTERVAL '10 days', 'Barangay Plaza'),
  ('Youth Camp 2026',
   'Three days of encounter with God. Registration opens soon.',
   NOW() + INTERVAL '30 days', 'Camp Grace Retreat Center')
) AS v(title, description, event_date, location)
WHERE NOT EXISTS (
  SELECT 1 FROM events WHERE event_date >= NOW()
);
