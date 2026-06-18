
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
      FROM pg_publication_tables
     WHERE pubname     = 'supabase_realtime'
       AND schemaname  = 'public'
       AND tablename   = 'user_notes'
  ) THEN
    EXECUTE 'ALTER PUBLICATION supabase_realtime ADD TABLE public.user_notes';
  END IF;
END $$;

