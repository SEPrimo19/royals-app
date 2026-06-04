-- =============================================================================
-- GRACE — Security Phase A.2: per-user RPC + write rate limiting
--
-- Why this matters
-- ----------------
-- Phase A.1 stops a tampered client from claiming "+9999 per attempt", but
-- it doesn't stop a scripted client from posting 1,000 max-legal attempts
-- per second to inflate the leaderboard or drain server resources. Same
-- shape: `use_lifeline` could be spammed to burn through balance / DB
-- writes faster than the daily refill caps imagine.
--
-- Approach
-- --------
-- One shared `rpc_call_log` table tracks (user_id, fn_name, called_at) for
-- every rate-limited surface. A helper `enforce_rate_limit(fn, max, window)`
-- counts recent rows + raises an exception if over budget. We call it at
-- the top of every rate-limited RPC, and a BEFORE INSERT trigger on
-- `game_attempts` enforces the same for the direct-table-insert path
-- (recordAttempt is not an RPC; it's a client INSERT).
--
-- Limits chosen
-- -------------
--   use_lifeline       → 10 calls / minute  (normal max is ~6 per day)
--   recordAttempt      → 60 inserts / min   (~1/sec sustained, far above
--                                            normal Practice play)
--
-- Legitimate gameplay stays well under both. Bots hit the wall fast.
--
-- Safe to re-run.
-- =============================================================================

-- ---- TABLE: rpc_call_log ------------------------------------------------
CREATE TABLE IF NOT EXISTS rpc_call_log (
  user_id   UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  fn_name   TEXT NOT NULL,
  called_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Composite index so the rate-limit COUNT query is index-only.
CREATE INDEX IF NOT EXISTS idx_rpc_call_log_user_fn_time
  ON rpc_call_log (user_id, fn_name, called_at DESC);

-- Log table is server-side bookkeeping — no app reads it. Lock it down.
ALTER TABLE rpc_call_log ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "rpc_call_log_no_access" ON rpc_call_log;
-- Empty USING means "no rows visible / writable via PostgREST". Server-side
-- SECURITY DEFINER functions bypass RLS as needed.
CREATE POLICY "rpc_call_log_no_access" ON rpc_call_log
  FOR ALL USING (false) WITH CHECK (false);

-- ---- HELPER: enforce_rate_limit -----------------------------------------
-- Raises 'Rate limit exceeded' if the caller has already issued
-- [max_calls] of [fn_name] in the last [window_seconds] seconds.
-- Otherwise records the call and returns. Authenticated callers only.
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
-- Authenticated role doesn't need to call this directly — only our SECURITY
-- DEFINER RPCs do. But triggers also run under the function's owner, so
-- we keep EXECUTE for postgres (the owner). No grant to authenticated.

-- ---- Wire into use_lifeline RPC -----------------------------------------
-- Re-create the function from v14 with the rate-limit check at the top.
-- Behavior is otherwise identical.
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

-- ---- TRIGGER on game_attempts (recordAttempt path) ----------------------
-- recordAttempt is a direct client INSERT, not an RPC, so we attach the
-- rate-limit check as a BEFORE INSERT trigger. 60 attempts/min/user is
-- comfortably above legitimate gameplay (~5–15 attempts/min sustained
-- across the busiest modes) but a bot hits the wall fast.
--
-- We DON'T run the check inside the existing `clamp_game_attempt_points`
-- trigger (A.1) — that one is pure data validation. Separation of concerns.
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

-- ---- Periodic cleanup recipe (optional, run manually as needed) ---------
-- The rpc_call_log table grows ~1 row per rate-limited call. At thousands
-- of users × hundreds of calls/day it'll add up. Prune anything older than
-- 1 day — the rate window is 60 seconds, so anything older is irrelevant.
--
--   DELETE FROM rpc_call_log WHERE called_at < NOW() - INTERVAL '1 day';
--
-- Schedule with pg_cron later if growth becomes a concern. For now, run
-- this manually every few weeks.

-- Sanity check (run manually):
--   -- 11th use_lifeline in 60s should fail:
--   SELECT use_lifeline('joshua');  -- repeat 11 times quickly
-- Expected: 11th call returns ERROR: Rate limit exceeded.
