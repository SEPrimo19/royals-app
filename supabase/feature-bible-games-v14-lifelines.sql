
ALTER TABLE game_user_stats
  ADD COLUMN IF NOT EXISTS joshua_remaining INTEGER NOT NULL DEFAULT 3;
ALTER TABLE game_user_stats
  ADD COLUMN IF NOT EXISTS daniel_remaining INTEGER NOT NULL DEFAULT 3;
ALTER TABLE game_user_stats
  ADD COLUMN IF NOT EXISTS lifelines_refilled_on DATE NOT NULL DEFAULT CURRENT_DATE;

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

