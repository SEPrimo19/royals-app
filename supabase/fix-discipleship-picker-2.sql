
CREATE OR REPLACE FUNCTION public.pick_todays_activity()
RETURNS TABLE (
  id            UUID,
  title         TEXT,
  description   TEXT,
  category      TEXT,
  duration_tag  TEXT
)
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  v_uid   UUID := auth.uid();
  v_today DATE := (NOW() AT TIME ZONE 'Asia/Manila')::DATE;
  v_swapped UUID[];
  v_skip_cats TEXT[];
BEGIN
  -- Today's swapped activity ids (so we don't re-pick what user explicitly skipped)
  SELECT activity_ids
    INTO v_swapped
    FROM user_discipleship_swaps
   WHERE user_id = v_uid AND swapped_date = v_today;
  -- No swap row today → SELECT INTO leaves the variable NULL, and
  -- `<> ALL (NULL)` would filter out EVERY activity. Normalize here.
  v_swapped := COALESCE(v_swapped, '{}');

  -- Categories user has completed in the last 3 days — variety hint
  SELECT COALESCE(array_agg(DISTINCT a.category), '{}')
    INTO v_skip_cats
    FROM user_discipleship_completions c
    JOIN discipleship_activities a ON a.id = c.activity_id
   WHERE c.user_id = v_uid
     AND c.completed_date >= v_today - INTERVAL '3 days';
  v_skip_cats := COALESCE(v_skip_cats, '{}');

  -- First try: skip swapped ids AND recent categories
  RETURN QUERY
    SELECT a.id, a.title, a.description, a.category, a.duration_tag
      FROM discipleship_activities a
     WHERE a.is_active
       AND a.id <> ALL (v_swapped)
       AND a.category <> ALL (v_skip_cats)
     ORDER BY random()
     LIMIT 1;

  IF FOUND THEN RETURN; END IF;

  -- Relaxed: drop the category filter (small library or active user)
  RETURN QUERY
    SELECT a.id, a.title, a.description, a.category, a.duration_tag
      FROM discipleship_activities a
     WHERE a.is_active
       AND a.id <> ALL (v_swapped)
     ORDER BY random()
     LIMIT 1;

  IF FOUND THEN RETURN; END IF;

  -- Wrap-around: user swapped through the whole library today. Ignore the
  -- swap list so "Pick another" keeps cycling instead of blanking the card.
  RETURN QUERY
    SELECT a.id, a.title, a.description, a.category, a.duration_tag
      FROM discipleship_activities a
     WHERE a.is_active
     ORDER BY random()
     LIMIT 1;
END;
$$;

GRANT EXECUTE ON FUNCTION public.pick_todays_activity() TO authenticated;
