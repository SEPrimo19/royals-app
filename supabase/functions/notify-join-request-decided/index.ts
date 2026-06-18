
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
  decided_note: string | null;
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
  const note = record["decided_note"];
  return {
    id, group_id, user_id, status,
    decided_note: typeof note === "string" ? note : null,
  };
}

Deno.serve(async (req) => {
  if (req.method !== "POST") return new Response("Method not allowed", { status: 405 });

  let payload: WebhookPayload;
  try {
    payload = await req.json();
  } catch {
    return new Response("Invalid JSON", { status: 400 });
  }

  if (payload.type !== "UPDATE" || payload.table !== "group_join_requests") {
    return new Response(
      JSON.stringify({ skipped: "non-update or wrong table" }),
      { status: 200, headers: { "Content-Type": "application/json" } },
    );
  }

  const newRow = parseRow(payload.record);
  const oldRow = parseRow(payload.old_record);
  if (!newRow || !oldRow) {
    return new Response("Malformed record", { status: 400 });
  }

  if (!(oldRow.status === "pending" &&
        (newRow.status === "approved" || newRow.status === "rejected"))) {
    return new Response(
      JSON.stringify({ skipped: "non-decision transition" }),
      { status: 200, headers: { "Content-Type": "application/json" } },
    );
  }

  const supabase = createClient(supabaseUrl, serviceKey, {
    auth: { persistSession: false },
  });

  if (await isAlreadyProcessed(
    supabase, `notify-join-request-decided:${newRow.id}:${newRow.status}`,
  )) {
    return ackSkipped("already-processed");
  }

  const { data: g } = await supabase
    .from("groups").select("name").eq("id", newRow.group_id).maybeSingle();
  const { data: u } = await supabase
    .from("users").select("fcm_token").eq("id", newRow.user_id).maybeSingle();

  if (!u?.fcm_token) {
    return new Response(
      JSON.stringify({ skipped: "requester has no fcm_token" }),
      { status: 200, headers: { "Content-Type": "application/json" } },
    );
  }

  const cellName = g?.name ?? "your cell";
  const title = newRow.status === "approved"
    ? "🎉 You're in!"
    : "Your join request was declined";
  const body = newRow.status === "approved"
    ? `Welcome to ${cellName}. Tap to meet your cell.`
    : (newRow.decided_note?.trim()
      ? `${cellName}: ${newRow.decided_note.trim().slice(0, 120)}`
      : `${cellName} couldn't approve right now. Try another cell or talk to your leader.`);

  const ok = await sendFcm({
    token: u.fcm_token as string,
    title,
    body,
    data: {
      channel: "community",
      destination: if_approved_life_group_else_find_cell(newRow.status),
      id: newRow.id,
    },
  });

  return new Response(
    JSON.stringify({ requester_pushed: ok }),
    { status: 200, headers: { "Content-Type": "application/json" } },
  );
});

function if_approved_life_group_else_find_cell(status: string): string {
  return status === "approved" ? "life_group" : "find_cell";
}
