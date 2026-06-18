


CREATE OR REPLACE FUNCTION public.clear_note_hearts_on_replace()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
  IF NEW.created_at IS DISTINCT FROM OLD.created_at THEN
    DELETE FROM user_note_hearts WHERE note_user_id = NEW.user_id;
  END IF;
  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_clear_note_hearts ON user_notes;
CREATE TRIGGER trg_clear_note_hearts
  AFTER UPDATE ON user_notes
  FOR EACH ROW
  EXECUTE FUNCTION public.clear_note_hearts_on_replace();


CREATE OR REPLACE FUNCTION public.clear_note_hearts_on_delete()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
  DELETE FROM user_note_hearts WHERE note_user_id = OLD.user_id;
  RETURN OLD;
END;
$$;

DROP TRIGGER IF EXISTS trg_clear_note_hearts_on_delete ON user_notes;
CREATE TRIGGER trg_clear_note_hearts_on_delete
  AFTER DELETE ON user_notes
  FOR EACH ROW
  EXECUTE FUNCTION public.clear_note_hearts_on_delete();


DELETE FROM user_note_hearts h
 WHERE NOT EXISTS (
         SELECT 1 FROM user_notes n WHERE n.user_id = h.note_user_id
       )
    OR h.created_at < (
         SELECT n.created_at FROM user_notes n WHERE n.user_id = h.note_user_id
       );
