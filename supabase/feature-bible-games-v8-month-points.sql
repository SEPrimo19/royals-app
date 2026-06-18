
CREATE OR REPLACE FUNCTION get_my_month_points()
RETURNS INTEGER
LANGUAGE plpgsql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  result INTEGER;
BEGIN
  IF auth.uid() IS NULL THEN
    RETURN 0;
  END IF;
  SELECT COALESCE(SUM(points_earned), 0)::INTEGER
    INTO result
    FROM game_attempts
   WHERE user_id = auth.uid()
     AND played_at >= date_trunc('month', NOW());
  RETURN result;
END;
$$;

REVOKE ALL ON FUNCTION get_my_month_points() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION get_my_month_points() TO authenticated;

