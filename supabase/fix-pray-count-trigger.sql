
CREATE OR REPLACE FUNCTION grace_sync_pray_count()
RETURNS TRIGGER AS $$
BEGIN
  IF (TG_OP = 'INSERT') THEN
    UPDATE prayers SET pray_count = pray_count + 1 WHERE id = NEW.prayer_id;
    RETURN NEW;
  ELSIF (TG_OP = 'DELETE') THEN
    UPDATE prayers SET pray_count = GREATEST(pray_count - 1, 0) WHERE id = OLD.prayer_id;
    RETURN OLD;
  END IF;
  RETURN NULL;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS trg_intercession_count ON prayer_intercessions;

CREATE TRIGGER trg_intercession_count
AFTER INSERT OR DELETE ON prayer_intercessions
FOR EACH ROW EXECUTE FUNCTION grace_sync_pray_count();

UPDATE prayers p
SET pray_count = (
  SELECT COUNT(*) FROM prayer_intercessions i WHERE i.prayer_id = p.id
);
