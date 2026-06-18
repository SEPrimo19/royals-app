
ALTER TABLE prayers
  ADD COLUMN IF NOT EXISTS posted_by_proxy UUID REFERENCES users(id);

ALTER TABLE user_meditation_submissions
  ADD COLUMN IF NOT EXISTS submitted_by_proxy UUID REFERENCES users(id);


DROP POLICY IF EXISTS "prayers_insert_proxy" ON prayers;
CREATE POLICY "prayers_insert_proxy" ON prayers
  FOR INSERT
  WITH CHECK (
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
    AND is_anonymous = FALSE
  );


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
