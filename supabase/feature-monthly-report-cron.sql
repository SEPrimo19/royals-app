
    CREATE EXTENSION IF NOT EXISTS pg_cron WITH SCHEMA extensions;
    CREATE EXTENSION IF NOT EXISTS pg_net  WITH SCHEMA extensions;

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

    DO $$
    BEGIN
      PERFORM cron.unschedule('notify-monthly-report-ready');
    EXCEPTION WHEN OTHERS THEN
      -- Not previously scheduled — that's fine.
      NULL;
    END $$;

    SELECT cron.schedule(
      'notify-monthly-report-ready',
      '0 1 * * *',
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

    GRANT EXECUTE ON FUNCTION public.is_day_before_last_sunday(DATE)
      TO postgres, service_role;
    GRANT EXECUTE ON FUNCTION public.next_monthly_report_push_date()
      TO postgres, service_role;
    GRANT EXECUTE ON FUNCTION public.notify_monthly_report_ready_now()
      TO postgres, service_role;
