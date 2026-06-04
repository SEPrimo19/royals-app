// Fans out an FCM push to every member with a registered fcm_token when
// a new prayer lands. Invoked by a Supabase Database Webhook (Dashboard →
// Database → Webhooks; see supabase/feature-prayer-push.sql for setup).
//
// Database Webhook payload shape:
//   {
//     type: "INSERT" | "UPDATE" | "DELETE",
//     table: string,
//     schema: string,
//     record: { ...new row... },
//     old_record: { ...old row... } | null
//   }
//
// notif_prayer_enabled lives in DataStore on the client, not in Postgres,
// so we fan out to every token; GraceFcmService.onMessageReceived checks
// the local toggle and drops the notification if the user has opted out.
// The poster's own device is excluded server-side.

import { createClient } from "https://esm.sh/@supabase/supabase-js@2";
import { ackSkipped, isAlreadyProcessed } from "../_shared/dedup.ts";
import { sendFcm } from "../_shared/fcm.ts";

type WebhookPayload = {
  type: "INSERT" | "UPDATE" | "DELETE";
  table: string;
  schema: string;
  record: Record<string, unknown> | null;
  old_record: Record<string, unknown> | null;
};

type PrayerRow = {
  id: string;
  user_id: string;
  content: string;
  is_anonymous: boolean;
};

const supabaseUrl = Deno.env.get("SUPABASE_URL")!;
const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;

function truncate(s: string, max = 80): string {
  if (s.length <= max) return s;
  return s.slice(0, max - 1).trimEnd() + "…";
}

function parsePrayer(record: Record<string, unknown> | null): PrayerRow | null {
  if (!record) return null;
  const id = record["id"];
  const user_id = record["user_id"];
  const content = record["content"];
  const is_anonymous = record["is_anonymous"];
  if (typeof id !== "string" || typeof user_id !== "string" ||
      typeof content !== "string" || typeof is_anonymous !== "boolean") {
    return null;
  }
  return { id, user_id, content, is_anonymous };
}

Deno.serve(async (req) => {
  if (req.method !== "POST") {
    return new Response("Method not allowed", { status: 405 });
  }

  let payload: WebhookPayload;
  try {
    payload = await req.json();
  } catch {
    console.error("notify-prayer-posted: invalid JSON in webhook body");
    return new Response("Invalid JSON", { status: 400 });
  }

  console.log(
    `notify-prayer-posted: type=${payload.type} table=${payload.table}`,
  );

  // Defensive: ignore anything but INSERTs on the prayers table.
  if (payload.type !== "INSERT" || payload.table !== "prayers") {
    return new Response(
      JSON.stringify({ skipped: "non-insert or wrong table" }),
      { status: 200, headers: { "Content-Type": "application/json" } },
    );
  }

  const prayer = parsePrayer(payload.record);
  if (!prayer) {
    console.error(
      "notify-prayer-posted: malformed record",
      JSON.stringify(payload.record),
    );
    return new Response("Malformed prayer record", { status: 400 });
  }

  const supabase = createClient(supabaseUrl, serviceKey, {
    auth: { persistSession: false },
  });

  // Idempotency guard — webhook retries shouldn't re-fan-out to every device.
  if (await isAlreadyProcessed(supabase, `notify-prayer-posted:${prayer.id}`)) {
    console.log(`notify-prayer-posted: prayer=${prayer.id} already processed — skip`);
    return ackSkipped("already-processed");
  }

  // Resolve the poster's display name only when needed — anonymous prayers
  // hide the name regardless of what the DB says, mirroring the client's
  // anonymous-safeguard pattern.
  let posterName: string | null = null;
  if (!prayer.is_anonymous) {
    const { data } = await supabase
      .from("users").select("name").eq("id", prayer.user_id).maybeSingle();
    posterName = (data?.name as string | null) ?? null;
  }

  // Pull every device token EXCEPT the poster's. Service role bypasses RLS,
  // which is what makes a cross-user fan-out possible.
  const { data: users, error } = await supabase
    .from("users")
    .select("id, fcm_token")
    .neq("id", prayer.user_id)
    .not("fcm_token", "is", null);

  if (error) {
    console.error(`notify-prayer-posted: users query failed: ${error.message}`);
    return new Response(`users query failed: ${error.message}`, { status: 500 });
  }

  const title = prayer.is_anonymous
    ? "🙏 New prayer request"
    : `🙏 ${posterName ?? "Someone"} asked for prayer`;
  const text = prayer.is_anonymous
    ? `Someone in your church family needs prayer — ${truncate(prayer.content)}`
    : truncate(prayer.content, 100);

  // Dedupe tokens — multiple accounts on one device share an FCM token, and
  // without this the same device gets N stacked pushes where N = accounts
  // on that device. With it, one push per unique device.
  const tokens = Array.from(new Set(
    (users ?? [])
      .map((u) => u.fcm_token as string | null)
      .filter((t): t is string => !!t && t.length > 0)
  ));

  console.log(
    `notify-prayer-posted: prayer=${prayer.id} anonymous=${prayer.is_anonymous} ` +
      `poster=${prayer.user_id} candidates=${tokens.length} (deduped)`,
  );

  if (tokens.length === 0) {
    console.warn(
      "notify-prayer-posted: NO CANDIDATES — every other user has fcm_token = NULL. " +
        "Make sure recipient devices have opened the app at least once so " +
        "GraceFcmService.onNewToken has written their token to users.fcm_token.",
    );
    return new Response(
      JSON.stringify({ candidates: 0, sent: 0 }),
      { status: 200, headers: { "Content-Type": "application/json" } },
    );
  }

  // Fire all sends in parallel — sendFcm swallows individual failures so
  // one stale token can't poison the batch.
  const results = await Promise.all(
    tokens.map((token) =>
      sendFcm({
        token,
        title,
        body: text,
        data: { channel: "prayer", destination: "prayer", id: prayer.id },
      })
    ),
  );

  const sent = results.filter(Boolean).length;
  console.log(
    `notify-prayer-posted: done — candidates=${tokens.length} sent=${sent}`,
  );
  return new Response(
    JSON.stringify({ candidates: tokens.length, sent }),
    { status: 200, headers: { "Content-Type": "application/json" } },
  );
});
