-- =============================================================================
-- GRACE — Feature: Google Sign-in / Signup
--
-- The OAuth flow only creates an `auth.users` row. This trigger mirrors that
-- into `public.users` so the app's domain model lights up immediately (role,
-- name, etc.) without the client having to detect "first-time Google user"
-- and INSERT manually.
--
-- The trigger is SECURITY DEFINER so it can write to public.users regardless
-- of the calling user's RLS — necessary because at INSERT time the row
-- doesn't exist yet and policies can't pass.
--
-- Safe to re-run. ON CONFLICT DO NOTHING avoids clobbering an existing row
-- (e.g., a member who previously signed up with email/password then linked
-- a Google identity).
--
-- Run in: Supabase Dashboard → SQL Editor → paste → Run.
-- =============================================================================

CREATE OR REPLACE FUNCTION public.handle_new_user() RETURNS TRIGGER
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
BEGIN
  -- Phase P.5 (Leader Proxy Mode claim flow) safety: if a proxy-only row
  -- already holds this email (because a leader pre-registered the user via
  -- "+ Add Member"), we MUST skip the insert. Otherwise the email UNIQUE
  -- constraint throws here, the trigger crashes, and Supabase Auth returns
  -- 500 to the client — making signup impossible for any pre-registered
  -- email. The ClaimRecord screen post-signin will merge the proxy data
  -- into the new auth identity.
  IF EXISTS (
    SELECT 1 FROM public.users
    WHERE LOWER(email) = LOWER(NEW.email)
      AND is_proxy_only = TRUE
  ) THEN
    RETURN NEW;
  END IF;

  INSERT INTO public.users (id, email, name, role)
  VALUES (
    NEW.id,
    NEW.email,
    -- Google sends the display name in raw_user_meta_data.full_name; fall
    -- back to email if any link is missing. Never NULL — column is NOT NULL.
    COALESCE(
      NEW.raw_user_meta_data->>'full_name',
      NEW.raw_user_meta_data->>'name',
      split_part(NEW.email, '@', 1)
    ),
    'member'
  )
  ON CONFLICT (id) DO NOTHING;
  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
  AFTER INSERT ON auth.users
  FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();
