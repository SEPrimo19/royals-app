
CREATE EXTENSION IF NOT EXISTS pg_cron;


CREATE OR REPLACE FUNCTION public.sweep_expired_join_requests()
RETURNS INT
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  v_count INT;
BEGIN
  WITH expired AS (
    UPDATE group_join_requests
       SET status      = 'expired',
           decided_at  = NOW()
     WHERE status      = 'pending'
       AND created_at  < NOW() - INTERVAL '30 days'
    RETURNING 1
  )
  SELECT count(*) INTO v_count FROM expired;
  RETURN v_count;
END;
$$;

GRANT EXECUTE ON FUNCTION public.sweep_expired_join_requests()
  TO postgres, service_role;


DO $$
BEGIN
  PERFORM cron.unschedule('sweep-expired-join-requests');
EXCEPTION WHEN OTHERS THEN
  NULL;
END $$;

SELECT cron.schedule(
  'sweep-expired-join-requests',
  '30 2 * * *',
  $cron$SELECT public.sweep_expired_join_requests();$cron$
);


