
ALTER TABLE checkins
  ADD COLUMN IF NOT EXISTS week_start DATE;

UPDATE checkins
   SET week_start = date_trunc('week', submitted_at)::date
 WHERE week_start IS NULL;

CREATE OR REPLACE FUNCTION set_checkin_week_start()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
  NEW.week_start := date_trunc(
    'week', COALESCE(NEW.submitted_at, NOW())
  )::date;
  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_set_checkin_week_start ON checkins;
CREATE TRIGGER trg_set_checkin_week_start
  BEFORE INSERT OR UPDATE ON checkins
  FOR EACH ROW
  EXECUTE FUNCTION set_checkin_week_start();

DELETE FROM checkins a
  USING checkins b
 WHERE a.user_id    = b.user_id
   AND a.week_start = b.week_start
   AND a.submitted_at < b.submitted_at;

ALTER TABLE checkins
  ALTER COLUMN week_start SET NOT NULL;

ALTER TABLE checkins
  DROP CONSTRAINT IF EXISTS checkins_user_week_unique;
ALTER TABLE checkins
  ADD CONSTRAINT checkins_user_week_unique
  UNIQUE (user_id, week_start);

