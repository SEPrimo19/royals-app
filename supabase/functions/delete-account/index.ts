
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const supabaseUrl = Deno.env.get("SUPABASE_URL")!;
const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;

Deno.serve(async (req) => {
  if (req.method !== "POST") {
    return new Response("Method not allowed", { status: 405 });
  }

  const auth = req.headers.get("authorization") ?? "";
  const jwt = auth.startsWith("Bearer ") ? auth.slice(7) : "";
  if (!jwt) {
    console.error("delete-account: missing Authorization header");
    return new Response("Unauthorized", { status: 401 });
  }

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

  const admin = createClient(supabaseUrl, serviceKey, {
    auth: { persistSession: false },
  });

  const { error: pubErr } = await admin.from("users").delete().eq("id", uid);
  if (pubErr) {
    console.error(`delete-account: public.users delete failed for ${uid}:`, pubErr);
  }

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
