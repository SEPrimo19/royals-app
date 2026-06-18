
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

type Row = {
  id: string;
  group_id: string;
  user_id: string;
  status: string;
};

const supabaseUrl = Deno.env.get("SUPABASE_URL")!;
const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;

function parseRow(record: Record<string, unknown> | null): Row | null {
  if (!record) return null;
  const id = record["id"];
  const group_id = record["group_id"];
  const user_id = record["user_id"];
  const status = record["status"];
  if (typeof id !== "string" || typeof group_id !== "string" ||
      typeof user_id !== "string" || typeof status !== "string") {
    return null;
  }
  return { id, group_id, user_id, status };
}

Deno.serve(async (req) => {
  if (req.method !== "POST") return new Response("Method not allowed", { status: 405 });

  let payload: WebhookPayload;
  try {
    payload = await req.json();
  } catch {
    return new Response("Invalid JSON", { status: 400 });
  }

  if (payload.type !== "INSERT" || payload.table !== "group_join_requests") {
    return new Response(
      JSON.stringify({ skipped: "non-insert or wrong table" }),
      { status: 200, headers: { "Content-Type": "application/json" } },
    );
  }

  const row = parseRow(payload.record);
  if (!row) {
    console.error("notify-join-request-created: malformed record");
    return new Response("Malformed record", { status: 400 });
  }
  if (row.status !== "pending") {
    return new Response(
      JSON.stringify({ skipped: "non-pending insert" }),
      { status: 200, headers: { "Content-Type": "application/json" } },
    );
  }

  const supabase = createClient(supabaseUrl, serviceKey, {
    auth: { persistSession: false },
  });

  if (await isAlreadyProcessed(supabase, `notify-join-request-created:${row.id}`)) {
    return ackSkipped("already-processed");
  }

  const { data: g } = await supabase
    .from("groups")
    .select("name, leader_id")
    .eq("id", row.group_id)
    .maybeSingle();

  if (!g || !g.leader_id) {
    console.log(`notify-join-request-created: group=${row.group_id} has no leader — skip`);
    return new Response(
      JSON.stringify({ skipped: "no leader" }),
      { status: 200, headers: { "Content-Type": "application/json" } },
    );
  }

  const { data: leader } = await supabase
    .from("users")
    .select("fcm_token")
    .eq("id", g.leader_id)
    .maybeSingle();

  if (!leader?.fcm_token) {
    return new Response(
      JSON.stringify({ skipped: "leader has no fcm_token" }),
      { status: 200, headers: { "Content-Type": "application/json" } },
    );
  }

  const { data: requester } = await supabase
    .from("users")
    .select("name")
    .eq("id", row.user_id)
    .maybeSingle();

  const requesterName = requester?.name ?? "Someone";
  const ok = await sendFcm({
    token: leader.fcm_token as string,
    title: `🤝 ${requesterName} wants to join your cell`,
    body: `Tap to review their request to join ${g.name}.`,
    data: {
      channel: "community",
      destination: "life_group",
      id: row.id,
    },
  });

  return new Response(
    JSON.stringify({ leader_pushed: ok }),
    { status: 200, headers: { "Content-Type": "application/json" } },
  );
});
