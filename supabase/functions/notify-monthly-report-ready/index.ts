// Fans out a "your monthly progress report is ready" push to every user
// with a registered fcm_token. Triggered by pg_cron (see
// supabase/feature-monthly-report-cron.sql) on the day before the last
// Sunday of each month, so members have time to generate + bring their
// report to the end-of-month cell-group meeting.
//
// Triggering options:
//   1. pg_cron via pg_net.http_post — sets the X-Cron-Secret header.
//   2. Manual invoke for testing — also requires X-Cron-Secret unless
//      the function is invoked with the service role JWT (Supabase
//      Dashboard "Invoke" button does this automatically).
//
// Dedup: keyed by YYYY-MM so a misconfigured cron that fires twice in
// one day still only sends one push per device per month.
//
// Client-side opt-out: notif_community_enabled lives in DataStore on the
// device. GraceFcmService.onMessageReceived already checks the toggle
// for the COMMUNITY channel and drops the notification if the user has
// opted out — no server-side filter required.

import { createClient } from "https://esm.sh/@supabase/supabase-js@2";
import { ackSkipped, isAlreadyProcessed } from "../_shared/dedup.ts";
import { sendFcm } from "../_shared/fcm.ts";

type Body = {
  // Optional override for testing — defaults to current month in PHT.
  period?: string; // "YYYY-MM"
};

const supabaseUrl = Deno.env.get("SUPABASE_URL")!;
const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
// CRON_SECRET is a shared random string between the pg_cron job and the
// function. If unset, the function falls back to requiring the Supabase
// service role JWT (which Supabase auto-attaches when invoking from the
// Dashboard or when verify_jwt = true).
const cronSecret = Deno.env.get("CRON_SECRET");

// Manila is UTC+8 with no DST. Compute "today in Manila" by shifting UTC
// forward 8 hours and reading the date parts. Avoids importing tz-data.
function manilaToday(): { year: number; month: number; ym: string } {
  const nowUtcMs = Date.now();
  const phMs = nowUtcMs + 8 * 60 * 60 * 1000;
  const d = new Date(phMs);
  // UTC getters now read the shifted clock as if it were UTC.
  const year = d.getUTCFullYear();
  const month = d.getUTCMonth() + 1; // 1-12
  const ym = `${year}-${String(month).padStart(2, "0")}`;
  return { year, month, ym };
}

const MONTH_NAMES = [
  "January", "February", "March", "April", "May", "June",
  "July", "August", "September", "October", "November", "December",
];

function monthLabel(ym: string): string {
  const [y, m] = ym.split("-").map(Number);
  if (!y || !m || m < 1 || m > 12) return ym;
  return `${MONTH_NAMES[m - 1]} ${y}`;
}

Deno.serve(async (req) => {
  if (req.method !== "POST") {
    return new Response("Method not allowed", { status: 405 });
  }

  // Shared-secret auth — only enforced when CRON_SECRET is configured.
  // pg_cron sends this header; the Supabase Dashboard "Invoke" button
  // doesn't, but it attaches the service role JWT instead and Supabase
  // gates the function at the platform layer in that case.
  if (cronSecret) {
    const provided = req.headers.get("x-cron-secret");
    if (provided !== cronSecret) {
      console.error(
        "notify-monthly-report-ready: missing or invalid X-Cron-Secret header",
      );
      return new Response("Forbidden", { status: 403 });
    }
  }

  let body: Body = {};
  // Empty body is fine — pg_cron sends a content-length: 0 POST when no
  // payload is configured.
  if ((req.headers.get("content-length") ?? "0") !== "0") {
    try {
      body = await req.json();
    } catch {
      console.error("notify-monthly-report-ready: invalid JSON body");
      return new Response("Invalid JSON", { status: 400 });
    }
  }

  const period = body.period ?? manilaToday().ym;
  const label = monthLabel(period);
  console.log(`notify-monthly-report-ready: period=${period} label=${label}`);

  const supabase = createClient(supabaseUrl, serviceKey, {
    auth: { persistSession: false },
  });

  // Idempotency: one push per month per device, even if the cron misfires.
  const dedupKey = `notify-monthly-report-ready:${period}`;
  if (await isAlreadyProcessed(supabase, dedupKey)) {
    console.log(`notify-monthly-report-ready: ${dedupKey} already processed — skip`);
    return ackSkipped("already-processed");
  }

  const { data: users, error } = await supabase
    .from("users")
    .select("id, fcm_token")
    .not("fcm_token", "is", null);

  if (error) {
    console.error(
      `notify-monthly-report-ready: users query failed: ${error.message}`,
    );
    return new Response(`users query failed: ${error.message}`, { status: 500 });
  }

  // Dedupe tokens — multiple accounts on a single device share one FCM
  // token. Without this, that device receives N stacked notifications.
  const tokens = Array.from(new Set(
    (users ?? [])
      .map((u) => u.fcm_token as string | null)
      .filter((t): t is string => !!t && t.length > 0),
  ));

  console.log(
    `notify-monthly-report-ready: candidates=${tokens.length} (deduped)`,
  );

  if (tokens.length === 0) {
    console.warn(
      "notify-monthly-report-ready: NO CANDIDATES — every user has " +
        "fcm_token = NULL. Open the app on at least one device so " +
        "GraceFcmService.onNewToken can write the token to users.fcm_token.",
    );
    return new Response(
      JSON.stringify({ period, candidates: 0, sent: 0 }),
      { status: 200, headers: { "Content-Type": "application/json" } },
    );
  }

  const title = "📊 Monthly progress report ready";
  const text =
    `Your ${label} attendance + meditation report is ready to generate. ` +
    "Tap to open My Progress.";

  const results = await Promise.all(
    tokens.map((token) =>
      sendFcm({
        token,
        title,
        body: text,
        data: {
          channel: "community",
          destination: "my_progress",
          period,
        },
      })
    ),
  );

  const sent = results.filter(Boolean).length;
  console.log(
    `notify-monthly-report-ready: done — period=${period} ` +
      `candidates=${tokens.length} sent=${sent}`,
  );
  return new Response(
    JSON.stringify({ period, candidates: tokens.length, sent }),
    { status: 200, headers: { "Content-Type": "application/json" } },
  );
});
