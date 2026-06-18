
CREATE EXTENSION IF NOT EXISTS pg_cron WITH SCHEMA extensions;

CREATE OR REPLACE FUNCTION public.purge_old_game_attempts()
RETURNS BIGINT
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  deleted BIGINT;
BEGIN
  -- Keep everything the weekly OR monthly board could still sum. A row is
  -- needed while played_at >= date_trunc('month') (monthly board) OR
  -- played_at >= date_trunc('week') (weekly board). So it is safe to drop
  -- only when it is older than BOTH — i.e. older than the EARLIER boundary.
  DELETE FROM game_attempts
   WHERE played_at < LEAST(
           date_trunc('month', NOW()),
           date_trunc('week',  NOW())
         );
  GET DIAGNOSTICS deleted = ROW_COUNT;
  RETURN deleted;
END;
$$;

REVOKE ALL ON FUNCTION public.purge_old_game_attempts() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.purge_old_game_attempts()
  TO postgres, service_role;

DO $$
BEGIN
  PERFORM cron.unschedule('purge-old-game-attempts');
EXCEPTION WHEN OTHERS THEN
  -- Not previously scheduled — fine.
  NULL;
END $$;

SELECT cron.schedule(
  'purge-old-game-attempts',
  '30 0 1 * *',
  $cron$ SELECT public.purge_old_game_attempts(); $cron$
);
