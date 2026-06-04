// Privileged account deletion. Called by the app's "Delete my account"
// button. Performs two deletes with the service role:
//
//   1. DELETE FROM public.users WHERE id = <caller>
//        → cascades through prayers / posts / messages / journal / game
//          attempts / lifelines / etc. via ON DELETE CASCADE
//        → soft-FK columns (created_by, leader_id, *_by_proxy) auto-null
//          via the SET NULL constraints (see security-phase-b-account-
//          deletion.sql)
//   2. supabase.auth.admin.deleteUser(<caller>)
//        → invalidates the JWT, removes the auth row, frees the email
//          for re-signup
//
// IMPORTANT: this function MUST be deployed WITHOUT --no-verify-jwt,
// unlike the other notification functions. The caller's identity is the
// thing we're trying to authenticate (we delete THEIR account, not
// arbitrary ones). The Supabase gateway verifies the JWT and the
// function reads `Authorization: Bearer <jwt>` to discover the caller.

import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const supabaseUrl = Deno.env.get("SUPABASE_URL")!;
const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;

Deno.serve(async (req) => {
  if (req.method !== "POST") {
    return new Response("Method not allowed", { status: 405 });
  }

  // 1) Resolve the caller from their JWT. The Supabase gateway has
  //    already verified the signature; we just need to decode the `sub`
  //    claim (= user uid) for the delete target.
  const auth = req.headers.get("authorization") ?? "";
  const jwt = auth.startsWith("Bearer ") ? auth.slice(7) : "";
  if (!jwt) {
    console.error("delete-account: missing Authorization header");
    return new Response("Unauthorized", { status: 401 });
  }

  // Use a per-request anon client to resolve the caller. Cheaper than
  // decoding the JWT manually and lets Supabase rotate its key without
  // us caring.
  const anonClient = createClient(
    supabaseUrl,
    Deno.env.get("SUPABASE_ANON_KEY")!,
    { global: { headers: { Authorization: `Bearer ${jwt}` } } },
  );
  const { data: userInfo, error: userErr } = await anonClient.auth.getUser();
  if (userErr || !userInfo?.user) {
    console.error("delete-account: getUser failed", userErr);
    return new Response("Invalid session", { status: 401 });
  }
  const uid = userInfo.user.id;
  console.log(`delete-account: caller=${uid}`);

  // 2) Service-role client for the privileged deletes.
  const admin = createClient(supabaseUrl, serviceKey, {
    auth: { persistSession: false },
  });

  // 2a) Delete the public.users row. Cascades through all the
  //     personal-data tables (prayers, posts, messages, journal, etc.).
  //     Soft-FK columns get nulled via the SET NULL constraints from
  //     security-phase-b-account-deletion.sql.
  const { error: pubErr } = await admin.from("users").delete().eq("id", uid);
  if (pubErr) {
    console.error(`delete-account: public.users delete failed for ${uid}:`, pubErr);
    // Don't bail — try the auth delete anyway so the user can't sign in
    // even if their data lingers. We'll return 500 at the end if either
    // step failed, so the app can show a "try again" message.
  }

  // 2b) Delete the auth row.
  const { error: authErr } = await admin.auth.admin.deleteUser(uid);
  if (authErr) {
    console.error(`delete-account: auth.admin.deleteUser failed for ${uid}:`, authErr);
  }

  if (pubErr || authErr) {
    return new Response(
      JSON.stringify({
        ok: false,
        public_users_error: pubErr?.message ?? null,
        auth_error: authErr?.message ?? null,
      }),
      { status: 500, headers: { "Content-Type": "application/json" } },
    );
  }

  console.log(`delete-account: ${uid} fully deleted`);
  return new Response(
    JSON.stringify({ ok: true }),
    { status: 200, headers: { "Content-Type": "application/json" } },
  );
});
