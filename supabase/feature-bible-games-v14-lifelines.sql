-- =============================================================================
-- GRACE — Bible Games v14: Lifelines (Joshua + Daniel power-ups)
--
-- Two power-ups:
--   🛡️ Joshua Effect — freeze the Trivia Practice countdown for the current
--                      question (applies only in modes with a timer)
--   🕯️ Daniel Effect — 50/50 — eliminate 2 wrong MCQ options in Trivia
--                      and Who Am I?
--
-- Economy: free daily allotment of 3 of each, single shared pool across
-- modes. Refills automatically on the first call after midnight (server
-- local time / UTC — whichever Postgres uses).
--
-- Tracking lives on `game_user_stats` (new columns). The two RPCs below
-- handle refill + decrement atomically so the client never has to know
-- about timestamps.
--
-- Safe to re-run.
-- =============================================================================

-- ---- COLUMNS: per-user remaining lifelines + refill date ----------------
ALTER TABLE game_user_stats
  ADD COLUMN IF NOT EXISTS joshua_remaining INTEGER NOT NULL DEFAULT 3;
ALTER TABLE game_user_stats
  ADD COLUMN IF NOT EXISTS daniel_remaining INTEGER NOT NULL DEFAULT 3;
ALTER TABLE game_user_stats
  ADD COLUMN IF NOT EXISTS lifelines_refilled_on DATE NOT NULL DEFAULT CURRENT_DATE;

-- ---- RPC: get_lifelines() ------------------------------------------------
-- Returns the caller's current remaining counts. Refills on read if the
-- stored date is earlier than today — so the first call after midnight
-- always resets to 3/3. This keeps the client read-only-honest: it never
-- has to mutate to get fresh counts.
CREATE OR REPLACE FUNCTION get_lifelines()
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
  -- Ensure row exists (a brand-new user might not have one yet).
  INSERT INTO game_user_stats (user_id)
    VALUES (uid)
    ON CONFLICT (user_id) DO NOTHING;
  -- Refill if stale.
  UPDATE game_user_stats
     SET joshua_remaining = 3,
         daniel_remaining = 3,
         lifelines_refilled_on = CURRENT_DATE
   WHERE user_id = uid
     AND lifelines_refilled_on < CURRENT_DATE;
  RETURN QUERY
    SELECT joshua_remaining, daniel_remaining
      FROM game_user_stats
     WHERE user_id = uid;
END;
$$;

REVOKE ALL ON FUNCTION get_lifelines() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION get_lifelines() TO authenticated;

-- ---- RPC: use_lifeline(kind) --------------------------------------------
-- Refills if stale, then decrements the requested kind by 1 atomically.
-- Returns the NEW remaining counts. Raises if already at 0 so the client
-- can show a "no lifelines left" message.
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
  INSERT INTO game_user_stats (user_id)
    VALUES (uid)
    ON CONFLICT (user_id) DO NOTHING;
  -- Refill on stale date first.
  UPDATE game_user_stats
     SET joshua_remaining = 3,
         daniel_remaining = 3,
         lifelines_refilled_on = CURRENT_DATE
   WHERE user_id = uid
     AND lifelines_refilled_on < CURRENT_DATE;
  -- Decrement the requested column, or raise if exhausted.
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

-- Sanity check (run manually — will fail in SQL Editor since auth.uid()
-- is NULL when running as the postgres role; works in-app):
--   SELECT * FROM get_lifelines();
