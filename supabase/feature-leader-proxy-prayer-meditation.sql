-- =============================================================================
-- Royals: The Kingdom Builders — Leader Proxy Prayer + Meditation (Phase P.3)
--
-- Lets cell leaders post prayers and log meditation reflections on behalf
-- of cell members who can't (no smartphone, paper journal handed in, etc.).
-- Builds on Phase P.1 (proxy-only users) and P.2 (proxy attendance).
--
-- Schema additions:
--   prayers.posted_by_proxy                     UUID — leader who posted
--   user_meditation_submissions.submitted_by_proxy  UUID — leader who logged
--
-- Visibility:
--   Prayer Wall + Life Feed: "(via {leader})" tag is rendered client-side
--   when posted_by_proxy IS NOT NULL — pastoral transparency.
--   Compliance PDFs: tag intentionally suppressed (pastoral dignity).
--
-- Safe to re-run.
-- =============================================================================

-- ---- COLUMNS ----------------------------------------------------------------
ALTER TABLE prayers
  ADD COLUMN IF NOT EXISTS posted_by_proxy UUID REFERENCES users(id);

ALTER TABLE user_meditation_submissions
  ADD COLUMN IF NOT EXISTS submitted_by_proxy UUID REFERENCES users(id);

-- ---- PRAYERS: leader proxy INSERT policy -----------------------------------
-- Original "prayers_insert" enforces auth.uid() = user_id — only the member
-- themselves could post. New policy is additive (Postgres OR's permissive
-- INSERT policies) so member-self path stays untouched.

DROP POLICY IF EXISTS "prayers_insert_proxy" ON prayers;
CREATE POLICY "prayers_insert_proxy" ON prayers
  FOR INSERT
  WITH CHECK (
    -- Can't impersonate: the proxy stamp must equal the caller.
    posted_by_proxy = auth.uid()
    AND EXISTS (
      SELECT 1 FROM users self
      WHERE self.id = auth.uid()
        AND self.role IN ('cell_leader','youth_president','pastor','admin')
        AND (
          self.role IN ('youth_president','pastor','admin')
          OR self.group_id = (
            SELECT group_id FROM users WHERE id = prayers.user_id
          )
        )
    )
    -- A proxy prayer cannot also be anonymous — the "(via leader)" tag
    -- would defeat the anonymity purpose, so we forbid the combination
    -- server-side as a defensive layer (UI also hides the toggle).
    AND is_anonymous = FALSE
  );

-- ---- MEDITATION SUBMISSIONS: leader proxy INSERT policy --------------------
-- Wrapping the existing submission insert path. The UNIQUE constraint
-- (user_id, meditation_id) still prevents duplicates if the member also
-- has the app and submitted themselves — leader proxy will fail loudly.

DROP POLICY IF EXISTS "meditation_subs_insert_proxy" ON user_meditation_submissions;
CREATE POLICY "meditation_subs_insert_proxy" ON user_meditation_submissions
  FOR INSERT
  WITH CHECK (
    submitted_by_proxy = auth.uid()
    AND EXISTS (
      SELECT 1 FROM users self
      WHERE self.id = auth.uid()
        AND self.role IN ('cell_leader','youth_president','pastor','admin')
        AND (
          self.role IN ('youth_president','pastor','admin')
          OR self.group_id = (
            SELECT group_id
            FROM users
            WHERE id = user_meditation_submissions.user_id
          )
        )
    )
  );

-- ---- MEDITATION SUBMISSIONS: leader proxy UPDATE ---------------------------
-- Leaders need to edit a proxy reflection (typo'd it, member added more
-- words later). Same scope rules.

DROP POLICY IF EXISTS "meditation_subs_update_proxy" ON user_meditation_submissions;
CREATE POLICY "meditation_subs_update_proxy" ON user_meditation_submissions
  FOR UPDATE
  USING (
    submitted_by_proxy IS NOT NULL
    AND EXISTS (
      SELECT 1 FROM users self
      WHERE self.id = auth.uid()
        AND self.role IN ('cell_leader','youth_president','pastor','admin')
        AND (
          self.role IN ('youth_president','pastor','admin')
          OR self.group_id = (
            SELECT group_id
            FROM users
            WHERE id = user_meditation_submissions.user_id
          )
        )
    )
  );

-- ---- Verification (silent if all good) --------------------------------------
DO $$
BEGIN
  PERFORM 1 FROM information_schema.columns
    WHERE table_name = 'prayers' AND column_name = 'posted_by_proxy';
  IF NOT FOUND THEN RAISE EXCEPTION 'prayers.posted_by_proxy missing'; END IF;
  PERFORM 1 FROM information_schema.columns
    WHERE table_name = 'user_meditation_submissions'
      AND column_name = 'submitted_by_proxy';
  IF NOT FOUND THEN
    RAISE EXCEPTION 'user_meditation_submissions.submitted_by_proxy missing';
  END IF;
END $$;
