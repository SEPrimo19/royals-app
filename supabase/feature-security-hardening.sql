
    DROP VIEW IF EXISTS weekly_group_leaderboard;

    ALTER FUNCTION public.grace_sync_pray_count()      SET search_path = public;
    ALTER FUNCTION public.log_role_change()            SET search_path = public;
    ALTER FUNCTION public.enforce_attendance_window()  SET search_path = public;
    ALTER FUNCTION public.handle_new_user()            SET search_path = public;

    REVOKE EXECUTE ON FUNCTION public.grace_sync_pray_count()
      FROM PUBLIC, anon, authenticated;
    REVOKE EXECUTE ON FUNCTION public.log_role_change()
      FROM PUBLIC, anon, authenticated;
    REVOKE EXECUTE ON FUNCTION public.enforce_attendance_window()
      FROM PUBLIC, anon, authenticated;
    REVOKE EXECUTE ON FUNCTION public.handle_new_user()
      FROM PUBLIC, anon, authenticated;

    REVOKE EXECUTE ON FUNCTION public.get_weekly_group_leaderboard(UUID, INTEGER)
      FROM anon;


    DROP POLICY IF EXISTS "challenges_select" ON challenges;
    DROP POLICY IF EXISTS "challenges_write"  ON challenges;
    CREATE POLICY "challenges_select" ON challenges
      FOR SELECT USING (auth.role() = 'authenticated');
    CREATE POLICY "challenges_write" ON challenges
      FOR ALL USING (
        EXISTS (
          SELECT 1 FROM users WHERE id = auth.uid()
            AND role IN ('youth_president','pastor','admin')
        )
      );

    DROP POLICY IF EXISTS "cp_select_self_or_leader" ON challenge_progress;
    DROP POLICY IF EXISTS "cp_write_self"            ON challenge_progress;
    CREATE POLICY "cp_select_self_or_leader" ON challenge_progress
      FOR SELECT USING (
        auth.uid() = user_id
        OR EXISTS (
          SELECT 1 FROM users WHERE id = auth.uid()
            AND role IN ('cell_leader','youth_president','pastor','admin')
        )
      );
    CREATE POLICY "cp_write_self" ON challenge_progress
      FOR ALL USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

    DROP POLICY IF EXISTS "notif_log_admin" ON notifications_log;
    CREATE POLICY "notif_log_admin" ON notifications_log
      FOR ALL USING (
        EXISTS (
          SELECT 1 FROM users WHERE id = auth.uid()
            AND role IN ('youth_president','pastor','admin')
        )
      );

    DROP POLICY IF EXISTS "rp_select" ON reading_plans;
    DROP POLICY IF EXISTS "rp_write"  ON reading_plans;
    CREATE POLICY "rp_select" ON reading_plans
      FOR SELECT USING (auth.role() = 'authenticated');
    CREATE POLICY "rp_write" ON reading_plans
      FOR ALL USING (
        EXISTS (
          SELECT 1 FROM users WHERE id = auth.uid()
            AND role I,N ('youth_president','pastor','admin')
        )
      );
