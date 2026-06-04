    -- =============================================================================
    -- GRACE — Security hardening (Supabase Advisor cleanup)
    --
    -- Addresses every actionable item from the Security Advisor:
    --   1. ERROR: weekly_group_leaderboard view is implicit SECURITY DEFINER → drop
    --   2. WARN:  mutable search_path on existing functions → pin to public
    --   3. WARN:  triggers exposed as RPCs to anon/authenticated → revoke EXECUTE
    --   4. WARN:  get_weekly_group_leaderboard callable by anon → revoke anon
    --   5. INFO:  RLS-enabled-no-policy on unused tables → add minimal policies
    --
    -- Manual (not in this file):
    --   - Leaked-password protection — toggle in Dashboard → Auth → Settings.
    --
    -- Safe to re-run.
    -- =============================================================================

    -- ---- 1. Drop the obsolete view --------------------------------------------
    -- v4 replaced this with the SECURITY DEFINER function get_weekly_group_leaderboard.
    -- The function does the same join + group-membership gate but keeps RLS happy.
    DROP VIEW IF EXISTS weekly_group_leaderboard;

    -- ---- 2. Lock search_path on existing SECURITY DEFINER + trigger funcs ----
    -- A mutable search_path lets a malicious schema shadow tables/functions the
    -- definer trusts. Pinning to `public` removes that attack surface. Idempotent.
    ALTER FUNCTION public.grace_sync_pray_count()      SET search_path = public;
    ALTER FUNCTION public.log_role_change()            SET search_path = public;
    ALTER FUNCTION public.enforce_attendance_window()  SET search_path = public;
    ALTER FUNCTION public.handle_new_user()            SET search_path = public;
    -- get_weekly_group_leaderboard already pins search_path in its definition.

    -- ---- 3. Revoke EXECUTE on trigger-only functions -------------------------
    -- These are invoked by Postgres triggers, never by REST. Granting EXECUTE
    -- to anon/authenticated lets anyone POST /rest/v1/rpc/<name> and run them
    -- in an arbitrary context. Lock them down completely.
    REVOKE EXECUTE ON FUNCTION public.grace_sync_pray_count()
      FROM PUBLIC, anon, authenticated;
    REVOKE EXECUTE ON FUNCTION public.log_role_change()
      FROM PUBLIC, anon, authenticated;
    REVOKE EXECUTE ON FUNCTION public.enforce_attendance_window()
      FROM PUBLIC, anon, authenticated;
    REVOKE EXECUTE ON FUNCTION public.handle_new_user()
      FROM PUBLIC, anon, authenticated;

    -- ---- 4. Tighten the leaderboard RPC --------------------------------------
    -- We need `authenticated` to call this (it's the whole point), but anon
    -- should never be able to. The function's own group/role check still
    -- gates which group an authenticated user can read.
    REVOKE EXECUTE ON FUNCTION public.get_weekly_group_leaderboard(UUID, INTEGER)
      FROM anon;
    -- The earlier v4 migration already GRANTed authenticated; that stays.

    -- ---- 5. Minimal RLS for unused tables ------------------------------------
    -- These tables exist in the schema but their features aren't wired up yet
    -- (challenges, reading plans, notification logs). RLS-enabled-no-policy
    -- means even authenticated users can't read them — which is fine UNTIL
    -- something tries to. Add policies that match the rest of the app's
    -- pattern so when those features ship they don't immediately break.

    -- challenges + challenge_progress: anyone signed-in reads; only senior
    -- leaders create/edit.
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

    -- notifications_log: senior-leader only. Used to be admin-only blast tool.
    DROP POLICY IF EXISTS "notif_log_admin" ON notifications_log;
    CREATE POLICY "notif_log_admin" ON notifications_log
      FOR ALL USING (
        EXISTS (
          SELECT 1 FROM users WHERE id = auth.uid()
            AND role IN ('youth_president','pastor','admin')
        )
      );

    -- reading_plans: anyone signed-in reads; senior leaders manage.
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
