-- =============================================================================
-- GRACE — Bug-fix migration (smoke-test pass #1)
--   #3 checkins RLS: schema enabled RLS on `checkins` but never added policies,
--      so all inserts were blocked ("new row violates row-level security policy").
--   #4 devotional empty: the original seed used CURRENT_DATE at run time, so
--      "today" no longer matches. Re-seed for the current date.
-- Safe to re-run.
-- =============================================================================

-- #3 ----------------------------------------------------------------------------
DROP POLICY IF EXISTS "checkins_insert_own" ON checkins;
DROP POLICY IF EXISTS "checkins_select_own" ON checkins;
DROP POLICY IF EXISTS "checkins_select_leader" ON checkins;

-- A youth submits their own check-in
CREATE POLICY "checkins_insert_own" ON checkins
  FOR INSERT WITH CHECK (auth.uid() = user_id);

-- A youth reads their own check-ins
CREATE POLICY "checkins_select_own" ON checkins
  FOR SELECT USING (auth.uid() = user_id);

-- The assigned leader (or any leader role) can read check-ins addressed to them
CREATE POLICY "checkins_select_leader" ON checkins
  FOR SELECT USING (
    auth.uid() = leader_id
    OR EXISTS (
      SELECT 1 FROM users
      WHERE id = auth.uid()
        AND role IN ('cell_leader','youth_president','pastor','admin')
    )
  );

-- #4 ----------------------------------------------------------------------------
-- Insert today's devotional if one doesn't already exist for today.
INSERT INTO devotionals
  (scheduled_date, title, verse_ref, verse_text, reflection, prayer_starter, journal_prompt)
SELECT
  CURRENT_DATE,
  'Anchored in Hope',
  'Jeremiah 29:11',
  'For I know the plans I have for you, declares the Lord, plans to prosper you and not to harm you, plans to give you hope and a future.',
  'God''s plans for you are good, even when the path is unclear. Today, rest in the truth that your future is held by a faithful God.',
  'Lord, thank You that Your plans for me are good. Help me trust You with what I cannot yet see...',
  'Where do you need to trust God''s plan over your own right now?'
WHERE NOT EXISTS (
  SELECT 1 FROM devotionals WHERE scheduled_date = CURRENT_DATE
);
