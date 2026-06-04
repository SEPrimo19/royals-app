-- =============================================================================
-- Royals: The Kingdom Builders — Leader Proxy Claim Flow (Phase P.5)
--
-- When a previously proxy-only member finally signs up (matching email),
-- the proxy users row holds the email under UNIQUE constraint — meaning
-- the app's signup code SKIPS creating a duplicate public.users row.
-- That leaves the new auth.users id with NO public.users row at all
-- until this RPC runs. The RPC therefore needs to handle BOTH:
--   (a) caller already has a public.users row (e.g. re-signup after dismiss)
--   (b) caller has no public.users row yet (the skip-at-signup case)
--
-- Either way the proxy row's data + all FKs migrate to caller_id atomically.
--
-- Authorization: SECURITY DEFINER bypasses RLS, but the function itself
-- re-checks that proxy_id is_proxy_only = TRUE AND the proxy's email
-- matches the caller's auth email. Without those guards an attacker could
-- claim arbitrary rows.
--
-- Safe to re-run.
-- =============================================================================

-- ---- Convenience: track claim history ---------------------------------------
-- claimed_by_user_id lets us audit which auth_id a proxy was merged into.
-- Not strictly required for the merge (we DELETE the row instead), but the
-- column is useful if we ever switch to soft-delete semantics for an audit
-- log. Nullable, no DEFAULT — meaningless on non-claimed rows.
ALTER TABLE users
  ADD COLUMN IF NOT EXISTS claimed_by_user_id UUID REFERENCES users(id);

-- ---- RPC --------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.claim_proxy_record(proxy_id UUID)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  caller_id      UUID := auth.uid();
  caller_email   TEXT;
  proxy_row      RECORD;
  caller_exists  BOOLEAN;
  migrated       JSONB := '{}'::JSONB;
  attendance_n   INT := 0;
  meditation_n   INT := 0;
  prayer_n       INT := 0;
  intercession_n INT := 0;
  rsvp_n         INT := 0;
BEGIN
  -- ---- Auth checks --------------------------------------------------------
  IF caller_id IS NULL THEN
    RAISE EXCEPTION 'Not signed in.';
  END IF;
  IF caller_id = proxy_id THEN
    RAISE EXCEPTION 'You can''t claim your own record.';
  END IF;

  -- Resolve caller's email from auth.users (the trusted source — the
  -- public.users row could in theory be tampered).
  SELECT email INTO caller_email
  FROM auth.users WHERE id = caller_id;
  IF caller_email IS NULL OR caller_email = '' THEN
    RAISE EXCEPTION 'Your account has no email on file.';
  END IF;

  -- Load + validate the proxy record. RECORD capture is important: we
  -- need to read its fields AFTER we delete the row in step 2.
  SELECT * INTO proxy_row FROM public.users WHERE id = proxy_id;
  IF NOT FOUND THEN
    RAISE EXCEPTION 'Proxy record not found.';
  END IF;
  IF NOT proxy_row.is_proxy_only THEN
    RAISE EXCEPTION 'That record is already an active account.';
  END IF;
  IF LOWER(COALESCE(proxy_row.email, '')) <> LOWER(caller_email) THEN
    RAISE EXCEPTION 'Email mismatch — that proxy record belongs to a different email.';
  END IF;

  -- Does the caller already have a public.users row? Drives the upsert
  -- branch below — (a) merge fields onto existing row, (b) create new row.
  SELECT EXISTS (SELECT 1 FROM public.users WHERE id = caller_id)
    INTO caller_exists;

  -- ---- Step 1: ensure the caller's public.users row exists FIRST ---------
  --
  -- The FK migrations below set event_attendance.user_id = caller_id (and
  -- similar). Postgres rejects that with 23503 if no row with id=caller_id
  -- exists yet. Since the handle_new_user trigger skips the insert when a
  -- proxy email collision is detected, the caller may not have a row.
  --
  -- We can't just INSERT — the proxy row still holds the email under the
  -- UNIQUE constraint. Workaround: temporarily NULL the proxy's email
  -- (the column is nullable as of Phase P.1), insert/upsert the caller's
  -- row with the freed email, then proceed. Both happen inside this
  -- transaction so external observers never see the half-state.
  UPDATE public.users SET email = NULL WHERE id = proxy_id;

  INSERT INTO public.users (
    id, email, name, role, group_id,
    is_compassion, compassion_number, emergency_contact,
    birthdate, sex,
    is_proxy_only, created_at
  ) VALUES (
    caller_id,
    COALESCE(proxy_row.email, caller_email),
    COALESCE(NULLIF(proxy_row.name, ''), split_part(caller_email, '@', 1)),
    'member',
    proxy_row.group_id,
    proxy_row.is_compassion,
    proxy_row.compassion_number,
    proxy_row.emergency_contact,
    proxy_row.birthdate,
    proxy_row.sex,
    FALSE,
    NOW()
  )
  ON CONFLICT (id) DO UPDATE SET
    -- Merge — caller's existing values win where set; otherwise inherit
    -- from the proxy. is_compassion ORs (a real account with Compassion
    -- ON shouldn't get downgraded by a proxy with Compassion OFF).
    birthdate         = COALESCE(public.users.birthdate, EXCLUDED.birthdate),
    sex               = COALESCE(public.users.sex, EXCLUDED.sex),
    is_compassion     = (public.users.is_compassion OR EXCLUDED.is_compassion),
    compassion_number = COALESCE(public.users.compassion_number, EXCLUDED.compassion_number),
    emergency_contact = COALESCE(
                          NULLIF(public.users.emergency_contact, ''),
                          NULLIF(EXCLUDED.emergency_contact, '')
                        ),
    group_id          = COALESCE(public.users.group_id, EXCLUDED.group_id),
    email             = COALESCE(NULLIF(public.users.email, ''), EXCLUDED.email);

  -- ---- Step 2: migrate FKs from proxy_id → caller_id ----------------------
  -- Caller row now exists, so these UPDATEs satisfy the FK references.
  UPDATE public.prayers SET user_id = caller_id WHERE user_id = proxy_id;
  GET DIAGNOSTICS prayer_n = ROW_COUNT;

  UPDATE public.prayer_intercessions SET user_id = caller_id
    WHERE user_id = proxy_id;
  GET DIAGNOSTICS intercession_n = ROW_COUNT;

  UPDATE public.event_attendance SET user_id = caller_id
    WHERE user_id = proxy_id;
  GET DIAGNOSTICS attendance_n = ROW_COUNT;

  UPDATE public.event_rsvp SET user_id = caller_id WHERE user_id = proxy_id;
  GET DIAGNOSTICS rsvp_n = ROW_COUNT;

  UPDATE public.user_meditation_submissions SET user_id = caller_id
    WHERE user_id = proxy_id;
  GET DIAGNOSTICS meditation_n = ROW_COUNT;

  UPDATE public.checkins SET user_id = caller_id WHERE user_id = proxy_id;
  UPDATE public.mood_checkins SET user_id = caller_id WHERE user_id = proxy_id;

  -- Composite-PK tables — upsert pattern with delete-from-proxy at the end.
  INSERT INTO public.group_members (user_id, group_id, joined_at)
    SELECT caller_id, group_id, joined_at
    FROM public.group_members WHERE user_id = proxy_id
  ON CONFLICT (user_id, group_id) DO NOTHING;
  DELETE FROM public.group_members WHERE user_id = proxy_id;

  INSERT INTO public.user_devo_progress (user_id, devo_id, completed_at, journal_entry)
    SELECT caller_id, devo_id, completed_at, journal_entry
    FROM public.user_devo_progress WHERE user_id = proxy_id
  ON CONFLICT (user_id, devo_id) DO NOTHING;
  DELETE FROM public.user_devo_progress WHERE user_id = proxy_id;

  UPDATE public.posts SET user_id = caller_id WHERE user_id = proxy_id;
  UPDATE public.reactions SET user_id = caller_id WHERE user_id = proxy_id;
  UPDATE public.comments SET user_id = caller_id WHERE user_id = proxy_id;

  -- ---- Step 3: drop the proxy row ----------------------------------------
  -- Any FK ON DELETE CASCADE rows that we missed above get cleaned here.
  -- Safe to delete now — its email was already nulled out in Step 1 and
  -- all FKs have been re-pointed at caller_id.
  DELETE FROM public.users WHERE id = proxy_id;

  -- ---- Step 4: return a summary the client can show in a toast ------------
  migrated := jsonb_build_object(
    'attendance', attendance_n,
    'meditations', meditation_n,
    'prayers', prayer_n,
    'intercessions', intercession_n,
    'rsvps', rsvp_n
  );
  RETURN jsonb_build_object(
    'success', true,
    'proxy_id', proxy_id,
    'caller_id', caller_id,
    'migrated', migrated
  );
END;
$$;

-- Authenticated users can call this — the function self-validates that
-- they're claiming a record matching their own email.
REVOKE ALL ON FUNCTION public.claim_proxy_record(UUID) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.claim_proxy_record(UUID) TO authenticated;

-- =============================================================================
-- Helper RPC for pre-signup detection
--
-- During signUp / signIn, the client needs to know whether a proxy-only
-- users row exists with a given email — BEFORE attempting to insert the
-- public.users row (which would otherwise hit a 409 on the UNIQUE email
-- constraint). RLS hides those rows from the just-authenticated user
-- (they can only see themselves + their group), so we expose a
-- SECURITY DEFINER RPC that takes an email and returns a minimal JSON
-- describing any match.
--
-- We deliberately return ONLY id + name + has_compassion + is_proxy_only
-- (no Compassion number, no group_id, no other PII) so a casual attacker
-- can't probe the function to harvest member data. The full record only
-- flows back via claim_proxy_record after the email-match check passes
-- there.
-- =============================================================================
CREATE OR REPLACE FUNCTION public.find_proxy_by_email(p_email TEXT)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  matches RECORD;
  cnt     INT;
BEGIN
  SELECT COUNT(*) INTO cnt
  FROM public.users
  WHERE is_proxy_only = TRUE
    AND LOWER(email) = LOWER(p_email);

  IF cnt = 0 THEN
    RETURN jsonb_build_object('found', false);
  END IF;
  IF cnt > 1 THEN
    -- Multiple matches — caller should surface an error and ask a
    -- leader to resolve the duplicate. Return without ids.
    RETURN jsonb_build_object('found', true, 'multiple', true);
  END IF;

  SELECT id, name, is_compassion INTO matches
  FROM public.users
  WHERE is_proxy_only = TRUE
    AND LOWER(email) = LOWER(p_email)
  LIMIT 1;

  RETURN jsonb_build_object(
    'found', true,
    'multiple', false,
    'proxy_id', matches.id,
    'name', matches.name,
    'is_compassion', matches.is_compassion
  );
END;
$$;

REVOKE ALL ON FUNCTION public.find_proxy_by_email(TEXT) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.find_proxy_by_email(TEXT) TO authenticated;

-- ---- Verification (silent on success) ---------------------------------------
DO $$
BEGIN
  PERFORM 1 FROM pg_proc WHERE proname = 'claim_proxy_record';
  IF NOT FOUND THEN RAISE EXCEPTION 'claim_proxy_record function missing'; END IF;
END $$;
