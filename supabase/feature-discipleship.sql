
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";


CREATE TABLE IF NOT EXISTS discipleship_activities (
  id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  title         TEXT NOT NULL CHECK (length(trim(title)) BETWEEN 1 AND 80),
  description   TEXT NOT NULL CHECK (length(trim(description)) BETWEEN 1 AND 600),
  category      TEXT NOT NULL CHECK (category IN (
                  'bible_study','prayer','witness','service','worship','character'
                )),
  duration_tag  TEXT NOT NULL DEFAULT '15min'
                  CHECK (duration_tag IN ('5min','15min','30min_plus')),
  is_active     BOOLEAN NOT NULL DEFAULT TRUE,
  created_by    UUID REFERENCES users(id),
  created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at    TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS user_discipleship_completions (
  user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  activity_id     UUID NOT NULL REFERENCES discipleship_activities(id) ON DELETE CASCADE,
  completed_date  DATE NOT NULL DEFAULT (NOW() AT TIME ZONE 'Asia/Manila')::DATE,
  reflection      TEXT,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  PRIMARY KEY (user_id, activity_id, completed_date)
);

CREATE TABLE IF NOT EXISTS user_discipleship_swaps (
  user_id        UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  swapped_date   DATE NOT NULL DEFAULT (NOW() AT TIME ZONE 'Asia/Manila')::DATE,
  activity_ids   UUID[] NOT NULL DEFAULT '{}',
  PRIMARY KEY (user_id, swapped_date)
);

CREATE INDEX IF NOT EXISTS idx_da_active_category
  ON discipleship_activities (is_active, category);
CREATE INDEX IF NOT EXISTS idx_udc_user_date
  ON user_discipleship_completions (user_id, completed_date);


ALTER TABLE discipleship_activities          ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_discipleship_completions    ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_discipleship_swaps          ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS da_select ON discipleship_activities;
DROP POLICY IF EXISTS da_write_leader ON discipleship_activities;
DROP POLICY IF EXISTS udc_select_own ON user_discipleship_completions;
DROP POLICY IF EXISTS udc_write_own ON user_discipleship_completions;
DROP POLICY IF EXISTS uds_rw_own ON user_discipleship_swaps;

CREATE POLICY da_select ON discipleship_activities
  FOR SELECT USING (auth.role() = 'authenticated');

CREATE POLICY da_write_leader ON discipleship_activities
  FOR ALL USING (
    EXISTS (
      SELECT 1 FROM users
      WHERE id = auth.uid()
        AND role IN ('cell_leader','council','youth_president','pastor','admin')
    )
  ) WITH CHECK (
    EXISTS (
      SELECT 1 FROM users
      WHERE id = auth.uid()
        AND role IN ('cell_leader','council','youth_president','pastor','admin')
    )
  );

CREATE POLICY udc_select_own ON user_discipleship_completions
  FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY udc_write_own ON user_discipleship_completions
  FOR ALL USING (auth.uid() = user_id)
              WITH CHECK (auth.uid() = user_id);

CREATE POLICY uds_rw_own ON user_discipleship_swaps
  FOR ALL USING (auth.uid() = user_id)
              WITH CHECK (auth.uid() = user_id);


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


CREATE OR REPLACE FUNCTION public.swap_todays_activity(p_activity_id UUID)
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  v_today DATE := (NOW() AT TIME ZONE 'Asia/Manila')::DATE;
BEGIN
  INSERT INTO user_discipleship_swaps (user_id, swapped_date, activity_ids)
  VALUES (auth.uid(), v_today, ARRAY[p_activity_id])
  ON CONFLICT (user_id, swapped_date)
  DO UPDATE SET activity_ids =
    (SELECT ARRAY(SELECT DISTINCT unnest(user_discipleship_swaps.activity_ids || EXCLUDED.activity_ids)));
END;
$$;

GRANT EXECUTE ON FUNCTION public.swap_todays_activity(UUID) TO authenticated;


CREATE OR REPLACE FUNCTION public.cell_completion_count_today()
RETURNS BIGINT
LANGUAGE sql
SECURITY DEFINER
STABLE
SET search_path = public
AS $$
  WITH my_group AS (
    SELECT group_id FROM users WHERE id = auth.uid()
  )
  SELECT COUNT(DISTINCT c.user_id)
    FROM user_discipleship_completions c
    JOIN users u ON u.id = c.user_id
   WHERE u.group_id IS NOT NULL
     AND u.group_id = (SELECT group_id FROM my_group)
     AND u.id <> auth.uid()  -- exclude self so the count reads "OTHERS in your cell"
     AND c.completed_date = (NOW() AT TIME ZONE 'Asia/Manila')::DATE;
$$;

GRANT EXECUTE ON FUNCTION public.cell_completion_count_today() TO authenticated;


CREATE OR REPLACE FUNCTION public.my_discipleship_streak()
RETURNS INT
LANGUAGE plpgsql
SECURITY DEFINER
STABLE
SET search_path = public
AS $$
DECLARE
  v_uid UUID := auth.uid();
  v_today DATE := (NOW() AT TIME ZONE 'Asia/Manila')::DATE;
  v_cursor DATE := v_today;
  v_count INT := 0;
  v_done BOOLEAN;
  v_grace BOOLEAN := TRUE;  -- allow today to be incomplete without breaking
BEGIN
  LOOP
    SELECT EXISTS (
      SELECT 1 FROM user_discipleship_completions
       WHERE user_id = v_uid AND completed_date = v_cursor
    ) INTO v_done;
    IF v_done THEN
      v_count := v_count + 1;
      v_grace := FALSE;
    ELSIF v_grace THEN
      v_grace := FALSE;  -- consume the today-grace, look at yesterday
    ELSE
      EXIT;
    END IF;
    v_cursor := v_cursor - 1;
    -- Hard cap at 365 days to bound iteration time.
    IF v_today - v_cursor > 365 THEN EXIT; END IF;
  END LOOP;
  RETURN v_count;
END;
$$;

GRANT EXECUTE ON FUNCTION public.my_discipleship_streak() TO authenticated;
