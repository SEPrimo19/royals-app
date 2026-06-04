    -- ============================================================
    -- Phase 9: Monthly compliance report push notification (cron)
    --
    -- Fires the notify-monthly-report-ready Edge Function once a month,
    -- on the day BEFORE the last Sunday — so members have Saturday to
    -- generate and bring their report to the Sunday cell-group meeting.
    --
    -- Mechanism:
    --   pg_cron runs DAILY at 01:00 UTC (09:00 PHT). The cron body checks
    --   public.is_day_before_last_sunday(CURRENT_DATE in PHT). On match,
    --   it POSTs to the Edge Function via pg_net.http_post with a shared
    --   secret in the X-Cron-Secret header.
    --
    --   Daily-with-guard is simpler and more transparent than trying to
    --   express "day before last Sunday" in a cron expression — the helper
    --   function is unit-testable in psql with a single SELECT.
    -- ============================================================
    --
    -- ONE-TIME DEPLOYMENT (in this order):
    --
    -- 1. Deploy the Edge Function:
    --      supabase functions deploy notify-monthly-report-ready
    --
    -- 2. Make sure FCM_SERVICE_ACCOUNT_JSON is already set as a secret
    --    (it should be, from Phase 6c prayer-push setup). If not:
    --      supabase secrets set FCM_SERVICE_ACCOUNT_JSON="$(cat firebase-adminsdk.json)"
    --
    -- 3. Generate a random shared secret on your machine, e.g.:
    --      openssl rand -hex 32          → copy the 64-char string
    --
    -- 4. Set CRON_SECRET on the function (so the function will accept
    --    the cron POST):
    --      supabase secrets set CRON_SECRET="<the random hex string from step 3>"
    --
    -- 5. In a SQL editor (Supabase Dashboard → SQL Editor), store the
    --    SAME secret + your project URL in Vault so this cron job can
    --    read them at runtime:
    --
    --      SELECT vault.create_secret(
    --        '<the random hex string from step 3>',
    --        'cron_secret'
    --      );
    --      SELECT vault.create_secret(
    --        'https://<your-project-ref>.supabase.co',
    --        'project_url'
    --      );
    --
    --    To rotate later, use vault.update_secret(secret_id, new_value).
    --
    -- 6. Run this file end-to-end (Dashboard → SQL Editor → paste).
    --
    -- TESTING:
    --   - To fire manually for the current month:
    --       SELECT public.notify_monthly_report_ready_now();
    --   - To preview which day the next push will go out:
    --       SELECT public.next_monthly_report_push_date();
    --   - To unschedule:
    --       SELECT cron.unschedule('notify-monthly-report-ready');
    -- ============================================================

    -- ------------------------------------------------------------
    -- Extensions
    -- ------------------------------------------------------------
    -- pg_cron + pg_net live in the "extensions" schema on hosted Supabase.
    CREATE EXTENSION IF NOT EXISTS pg_cron WITH SCHEMA extensions;
    CREATE EXTENSION IF NOT EXISTS pg_net  WITH SCHEMA extensions;

    -- ------------------------------------------------------------
    -- Helper: is the given date the day before the last Sunday of its month?
    --
    -- Logic:
    --   1. tomorrow must be a Sunday (DOW = 0)
    --   2. tomorrow + 7 days must roll into the NEXT month — meaning there
    --      is no Sunday after tomorrow within the same month, so tomorrow
    --      is the LAST Sunday.
    --
    -- Examples (verifiable in psql):
    --   SELECT public.is_day_before_last_sunday('2026-06-27'); -- TRUE  (Sat before last Sun Jun 28)
    --   SELECT public.is_day_before_last_sunday('2026-06-20'); -- FALSE (Sat before 2nd-to-last Sun)
    --   SELECT public.is_day_before_last_sunday('2026-12-26'); -- TRUE  (Sat before last Sun Dec 27)
    -- ------------------------------------------------------------
    CREATE OR REPLACE FUNCTION public.is_day_before_last_sunday(d DATE)
    RETURNS BOOLEAN
    LANGUAGE plpgsql
    IMMUTABLE
    AS $$
    DECLARE
      next_day DATE := d + 1;
    BEGIN
      -- next_day must be Sunday (EXTRACT DOW returns 0 for Sunday)
      IF EXTRACT(DOW FROM next_day)::INT <> 0 THEN
        RETURN FALSE;
      END IF;
      -- next_day must be the LAST Sunday — i.e., +7 days lands in a new month
      IF EXTRACT(MONTH FROM next_day + 7) = EXTRACT(MONTH FROM next_day) THEN
        RETURN FALSE;
      END IF;
      RETURN TRUE;
    END;
    $$;

    -- Preview when the next push will go out (handy for sanity-checking).
    CREATE OR REPLACE FUNCTION public.next_monthly_report_push_date()
    RETURNS DATE
    LANGUAGE plpgsql
    STABLE
    AS $$
    DECLARE
      d DATE := (now() AT TIME ZONE 'Asia/Manila')::date;
      i INT  := 0;
    BEGIN
      WHILE i < 60 LOOP
        IF public.is_day_before_last_sunday(d) THEN
          RETURN d;
        END IF;
        d := d + 1;
        i := i + 1;
      END LOOP;
      RETURN NULL; -- shouldn't happen — every month has a last Sunday
    END;
    $$;

    -- ------------------------------------------------------------
    -- The work: POST to the Edge Function. Wrapped in a function so the
    -- cron body stays one line and so we can re-invoke manually for testing.
    -- ------------------------------------------------------------
    CREATE OR REPLACE FUNCTION public.notify_monthly_report_ready_now()
    RETURNS BIGINT
    LANGUAGE plpgsql
    SECURITY DEFINER
    SET search_path = public, net, vault
    AS $$
    DECLARE
      url    TEXT;
      secret TEXT;
      req_id BIGINT;
    BEGIN
      SELECT decrypted_secret INTO url
      FROM vault.decrypted_secrets WHERE name = 'project_url';
      SELECT decrypted_secret INTO secret
      FROM vault.decrypted_secrets WHERE name = 'cron_secret';

      IF url IS NULL OR secret IS NULL THEN
        RAISE EXCEPTION
          'Missing vault secret(s): project_url and/or cron_secret. '
          'See deployment steps at the top of feature-monthly-report-cron.sql.';
      END IF;

      -- pg_net lives in the `net` schema (not `extensions`). The default
      -- timeout is 1000ms which can clip the FCM fan-out — bump it.
      SELECT net.http_post(
        url     := url || '/functions/v1/notify-monthly-report-ready',
        headers := jsonb_build_object(
                     'Content-Type',   'application/json',
                     'X-Cron-Secret',  secret
                   ),
        body    := '{}'::jsonb,
        timeout_milliseconds := 30000
      ) INTO req_id;

      RETURN req_id;
    END;
    $$;

    -- ------------------------------------------------------------
    -- Cron schedule — runs DAILY at 01:00 UTC = 09:00 Asia/Manila.
    -- The guard inside the body ensures only the qualifying day actually
    -- POSTs; on every other day this is a single CASE/SELECT, essentially
    -- free.
    -- ------------------------------------------------------------
    -- Unschedule any prior version of this job to make this file safe to
    -- re-run (cron.schedule errors on duplicate job names).
    DO $$
    BEGIN
      PERFORM cron.unschedule('notify-monthly-report-ready');
    EXCEPTION WHEN OTHERS THEN
      -- Not previously scheduled — that's fine.
      NULL;
    END $$;

    SELECT cron.schedule(
      'notify-monthly-report-ready',
      '0 1 * * *',  -- 01:00 UTC daily = 09:00 Asia/Manila
      $cron$
      SELECT CASE
        WHEN public.is_day_before_last_sunday(
          (now() AT TIME ZONE 'Asia/Manila')::date
        )
        THEN public.notify_monthly_report_ready_now()
        ELSE NULL
      END;
      $cron$
    );

    -- ------------------------------------------------------------
    -- Grant: pg_cron runs jobs as the cron role, which needs EXECUTE on
    -- the helper functions. Service role is included for manual testing
    -- from the SQL editor.
    -- ------------------------------------------------------------
    GRANT EXECUTE ON FUNCTION public.is_day_before_last_sunday(DATE)
      TO postgres, service_role;
    GRANT EXECUTE ON FUNCTION public.next_monthly_report_push_date()
      TO postgres, service_role;
    GRANT EXECUTE ON FUNCTION public.notify_monthly_report_ready_now()
      TO postgres, service_role;
