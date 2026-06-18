
CREATE EXTENSION IF NOT EXISTS pg_cron;
CREATE EXTENSION IF NOT EXISTS pg_net;


CREATE OR REPLACE FUNCTION public.birthdays_today_ph(
  p_month INT,
  p_day   INT
)
RETURNS TABLE (
  id        UUID,
  name      TEXT,
  fcm_token TEXT,
  group_id  UUID
)
LANGUAGE sql
SECURITY DEFINER
SET search_path = public
STABLE
AS $$
  SELECT u.id, u.name, u.fcm_token, u.group_id
  FROM   public.users u
  WHERE  u.birthdate IS NOT NULL
    -- Skip proxy-only members — no device, no point pushing. Their cell
    -- leader still sees the birthday in the leader UI / member detail.
    AND  COALESCE(u.is_proxy_only, FALSE) = FALSE
    AND  EXTRACT(MONTH FROM u.birthdate)::INT = p_month
    AND  EXTRACT(DAY   FROM u.birthdate)::INT = p_day;
$$;

GRANT EXECUTE ON FUNCTION public.birthdays_today_ph(INT, INT)
  TO postgres, service_role;


CREATE OR REPLACE FUNCTION public.notify_birthdays_now()
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
      'See deployment steps at the top of feature-birthday-cron.sql.';
  END IF;

  SELECT net.http_post(
    url     := url || '/functions/v1/notify-birthdays',
    headers := jsonb_build_object(
                 'Content-Type',  'application/json',
                 'X-Cron-Secret', secret
               ),
    body    := '{}'::jsonb,
    timeout_milliseconds := 30000
  ) INTO req_id;

  RETURN req_id;
END;
$$;

GRANT EXECUTE ON FUNCTION public.notify_birthdays_now()
  TO postgres, service_role;


DO $$
BEGIN
  PERFORM cron.unschedule('notify-birthdays-daily');
EXCEPTION WHEN OTHERS THEN
  NULL;  -- not previously scheduled — fine.
END $$;

SELECT cron.schedule(
  'notify-birthdays-daily',
  '0 0 * * *',
  $cron$SELECT public.notify_birthdays_now();$cron$
);
