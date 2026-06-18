
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

type EventRow = {
  id: string;
  title: string;
  event_date: string;
  location: string | null;
  created_by: string | null;
};

const supabaseUrl = Deno.env.get("SUPABASE_URL")!;
const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;

function parseEvent(record: Record<string, unknown> | null): EventRow | null {
  if (!record) return null;
  const id = record["id"];
  const title = record["title"];
  const event_date = record["event_date"];
  if (typeof id !== "string" || typeof title !== "string" ||
      typeof event_date !== "string") {
    return null;
  }
  const loc = record["location"];
  const cb = record["created_by"];
  return {
    id,
    title,
    event_date,
    location: typeof loc === "string" ? loc : null,
    created_by: typeof cb === "string" ? cb : null,
  };
}

function formatEventDate(iso: string): string {
  try {
    const d = new Date(iso);
    const fmt = new Intl.DateTimeFormat("en-US", {
      timeZone: "Asia/Manila",
      weekday: "short",
      month: "short",
      day: "numeric",
      hour: "numeric",
      minute: "2-digit",
      hour12: true,
    });
    return fmt.format(d).replace(",", " ·");
  } catch {
    return iso;
  }
}

Deno.serve(async (req) => {
  if (req.method !== "POST") {
    return new Response("Method not allowed", { status: 405 });
  }

  let payload: WebhookPayload;
  try {
    payload = await req.json();
  } catch {
    console.error("notify-event-created: invalid JSON body");
    return new Response("Invalid JSON", { status: 400 });
  }

  console.log(
    `notify-event-created: type=${payload.type} table=${payload.table}`,
  );

  if (payload.type !== "INSERT" || payload.table !== "events") {
    return new Response(
      JSON.stringify({ skipped: "non-insert or wrong table" }),
      { status: 200, headers: { "Content-Type": "application/json" } },
    );
  }

  const event = parseEvent(payload.record);
  if (!event) {
    console.error(
      "notify-event-created: malformed record",
      JSON.stringify(payload.record),
    );
    return new Response("Malformed event record", { status: 400 });
  }

  const supabase = createClient(supabaseUrl, serviceKey, {
    auth: { persistSession: false },
  });

  if (await isAlreadyProcessed(supabase, `notify-event-created:${event.id}`)) {
    console.log(`notify-event-created: event=${event.id} already processed — skip`);
    return ackSkipped("already-processed");
  }

  let query = supabase
    .from("users")
    .select("id, fcm_token")
    .not("fcm_token", "is", null);
  if (event.created_by) {
    query = query.neq("id", event.created_by);
  }
  const { data: users, error } = await query;

  if (error) {
    console.error(`notify-event-created: users query failed: ${error.message}`);
    return new Response(`users query failed: ${error.message}`, { status: 500 });
  }

  const tokens = Array.from(new Set(
    (users ?? [])
      .map((u) => u.fcm_token as string | null)
      .filter((t): t is string => !!t && t.length > 0)
  ));

  console.log(
    `notify-event-created: event=${event.id} candidates=${tokens.length} (deduped)`,
  );

  if (tokens.length === 0) {
    return new Response(
      JSON.stringify({ candidates: 0, sent: 0 }),
      { status: 200, headers: { "Content-Type": "application/json" } },
    );
  }

  const when = formatEventDate(event.event_date);
  const title = `📅 New event: ${event.title}`;
  const body = event.location
    ? `${when} · ${event.location}`
    : when;

  const results = await Promise.all(
    tokens.map((token) =>
      sendFcm({
        token,
        title,
        body,
        data: { channel: "community", destination: "events", id: event.id },
      })
    ),
  );

  const sent = results.filter(Boolean).length;
  console.log(
    `notify-event-created: done — candidates=${tokens.length} sent=${sent}`,
  );
  return new Response(
    JSON.stringify({ candidates: tokens.length, sent }),
    { status: 200, headers: { "Content-Type": "application/json" } },
  );
});
