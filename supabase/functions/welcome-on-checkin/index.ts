
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

type AttendanceRow = {
  event_id: string;
  user_id: string;
  status: string | null;
  late_by_minutes: number | null;
};

const supabaseUrl = Deno.env.get("SUPABASE_URL")!;
const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;

function parseAttendance(
  record: Record<string, unknown> | null,
): AttendanceRow | null {
  if (!record) return null;
  const event_id = record["event_id"];
  const user_id = record["user_id"];
  if (typeof event_id !== "string" || typeof user_id !== "string") return null;
  const status = record["status"];
  const lateRaw = record["late_by_minutes"];
  return {
    event_id,
    user_id,
    status: typeof status === "string" ? status : null,
    late_by_minutes: typeof lateRaw === "number" ? lateRaw : null,
  };
}

Deno.serve(async (req) => {
  if (req.method !== "POST") {
    return new Response("Method not allowed", { status: 405 });
  }

  let payload: WebhookPayload;
  try {
    payload = await req.json();
  } catch {
    console.error("welcome-on-checkin: invalid JSON body");
    return new Response("Invalid JSON", { status: 400 });
  }

  console.log(
    `welcome-on-checkin: type=${payload.type} table=${payload.table}`,
  );

  if (payload.type !== "INSERT" || payload.table !== "event_attendance") {
    return new Response(
      JSON.stringify({ skipped: "non-insert or wrong table" }),
      { status: 200, headers: { "Content-Type": "application/json" } },
    );
  }

  const attendance = parseAttendance(payload.record);
  if (!attendance) {
    console.error(
      "welcome-on-checkin: malformed record",
      JSON.stringify(payload.record),
    );
    return new Response("Malformed attendance record", { status: 400 });
  }

  const supabase = createClient(supabaseUrl, serviceKey, {
    auth: { persistSession: false },
  });

  const dedupKey =
    `welcome-on-checkin:${attendance.event_id}:${attendance.user_id}`;
  if (await isAlreadyProcessed(supabase, dedupKey)) {
    console.log(
      `welcome-on-checkin: event=${attendance.event_id} user=${attendance.user_id} already processed — skip`,
    );
    return ackSkipped("already-processed");
  }

  const [userRes, eventRes] = await Promise.all([
    supabase.from("users")
      .select("name, fcm_token")
      .eq("id", attendance.user_id)
      .maybeSingle(),
    supabase.from("events")
      .select("title")
      .eq("id", attendance.event_id)
      .maybeSingle(),
  ]);

  if (userRes.error || eventRes.error) {
    const msg = userRes.error?.message ?? eventRes.error?.message;
    console.error(`welcome-on-checkin: lookup failed: ${msg}`);
    return new Response(`lookup failed: ${msg}`, { status: 500 });
  }

  const user = userRes.data;
  const event = eventRes.data;

  if (!user || !event) {
    console.warn(
      `welcome-on-checkin: missing user (${!user}) or event (${!event}) — skipping`,
    );
    return new Response(
      JSON.stringify({ sent: false, reason: "user_or_event_missing" }),
      { status: 200, headers: { "Content-Type": "application/json" } },
    );
  }

  const fcmToken = (user.fcm_token as string | null) ?? "";
  if (!fcmToken) {
    console.warn(
      `welcome-on-checkin: user ${attendance.user_id} has no fcm_token`,
    );
    return new Response(
      JSON.stringify({ sent: false, reason: "user_has_no_fcm_token" }),
      { status: 200, headers: { "Content-Type": "application/json" } },
    );
  }

  const name = ((user.name as string | null) ?? "").trim() || "friend";
  const eventTitle = (event.title as string) ?? "the event";

  const isLate = attendance.status === "late";

  const title = `Welcome, ${name}! 🙏`;
  const body = isLate
    ? `Glad you made it to ${eventTitle}. Your presence matters.`
    : `Thank you for being here at ${eventTitle}. Your faithfulness is a blessing.`;

  const ok = await sendFcm({
    token: fcmToken,
    title,
    body,
    data: {
      channel: "community",
      destination: "events",
      id: attendance.event_id,
    },
  });

  console.log(
    `welcome-on-checkin: event=${attendance.event_id} user=${attendance.user_id} ` +
      `late=${isLate} sent=${ok}`,
  );

  return new Response(
    JSON.stringify({ sent: ok }),
    { status: 200, headers: { "Content-Type": "application/json" } },
  );
});
