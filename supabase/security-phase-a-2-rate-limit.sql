
CREATE TABLE IF NOT EXISTS rpc_call_log (
  user_id   UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  fn_name   TEXT NOT NULL,
  called_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_rpc_call_log_user_fn_time
  ON rpc_call_log (user_id, fn_name, called_at DESC);

ALTER TABLE rpc_call_log ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "rpc_call_log_no_access" ON rpc_call_log;
CREATE POLICY "rpc_call_log_no_access" ON rpc_call_log
  FOR ALL USING (false) WITH CHECK (false);

CREATE OR REPLACE FUNCTION enforce_rate_limit(
  fn_name TEXT,
  max_calls INTEGER,
  window_seconds INTEGER
)
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  uid UUID := auth.uid();
  recent_count INTEGER;
BEGIN
  IF uid IS NULL THEN
    RAISE EXCEPTION 'Authentication required';
  END IF;

  SELECT count(*)
    INTO recent_count
    FROM rpc_call_log
   WHERE user_id = uid
     AND rpc_call_log.fn_name = enforce_rate_limit.fn_name
     AND called_at >= NOW() - (window_seconds || ' seconds')::INTERVAL;

  IF recent_count >= max_calls THEN
    RAISE EXCEPTION
      'Rate limit exceeded: % calls of % allowed per % seconds',
      max_calls, enforce_rate_limit.fn_name, window_seconds;
  END IF;

  INSERT INTO rpc_call_log (user_id, fn_name)
    VALUES (uid, enforce_rate_limit.fn_name);
END;
$$;

REVOKE ALL ON FUNCTION enforce_rate_limit(TEXT, INTEGER, INTEGER) FROM PUBLIC;

CREATE OR REPLACE FUNCTION use_lifeline(kind TEXT)
RETURNS TABLE (joshua INTEGER, daniel INTEGER)
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  uid UUID := auth.uid();
BEGIN
  IF uid IS NULL THEN
    RAISE EXCEPTION 'Authentication required';
  END IF;
  IF kind NOT IN ('joshua','daniel') THEN
    RAISE EXCEPTION 'Unknown lifeline kind: %', kind;
  END IF;

  -- A.2 guard: cap any one user at 10 use_lifeline calls per 60 seconds.
  PERFORM enforce_rate_limit('use_lifeline', 10, 60);

  INSERT INTO game_user_stats (user_id)
    VALUES (uid)
    ON CONFLICT (user_id) DO NOTHING;
  UPDATE game_user_stats
     SET joshua_remaining = 3,
         daniel_remaining = 3,
         lifelines_refilled_on = CURRENT_DATE
   WHERE user_id = uid
     AND lifelines_refilled_on < CURRENT_DATE;
  IF kind = 'joshua' THEN
    UPDATE game_user_stats
       SET joshua_remaining = joshua_remaining - 1
     WHERE user_id = uid
       AND joshua_remaining > 0;
    IF NOT FOUND THEN
      RAISE EXCEPTION 'No Joshua Effects remaining today';
    END IF;
  ELSE
    UPDATE game_user_stats
       SET daniel_remaining = daniel_remaining - 1
     WHERE user_id = uid
       AND daniel_remaining > 0;
    IF NOT FOUND THEN
      RAISE EXCEPTION 'No Daniel Effects remaining today';
    END IF;
  END IF;
  RETURN QUERY
    SELECT joshua_remaining, daniel_remaining
      FROM game_user_stats
     WHERE user_id = uid;
END;
$$;

REVOKE ALL ON FUNCTION use_lifeline(TEXT) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION use_lifeline(TEXT) TO authenticated;

CREATE OR REPLACE FUNCTION rate_limit_game_attempt()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
  PERFORM enforce_rate_limit('recordAttempt', 60, 60);
  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_rate_limit_game_attempt ON game_attempts;
CREATE TRIGGER trg_rate_limit_game_attempt
  BEFORE INSERT ON game_attempts
  FOR EACH ROW
  EXECUTE FUNCTION rate_limit_game_attempt();


