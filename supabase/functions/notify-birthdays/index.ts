
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";
import { ackSkipped, isAlreadyProcessed } from "../_shared/dedup.ts";
import { sendFcm } from "../_shared/fcm.ts";

const supabaseUrl = Deno.env.get("SUPABASE_URL")!;
const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
const cronSecret = Deno.env.get("CRON_SECRET");

function manilaToday(): { mm: number; dd: number; ymd: string } {
  const phMs = Date.now() + 8 * 60 * 60 * 1000;
  const d = new Date(phMs);
  const yyyy = d.getUTCFullYear();
  const mm = d.getUTCMonth() + 1;
  const dd = d.getUTCDate();
  const ymd = `${yyyy}-${String(mm).padStart(2, "0")}-${String(dd).padStart(2, "0")}`;
  return { mm, dd, ymd };
}

type BirthdayUser = {
  id: string;
  name: string;
  fcm_token: string | null;
  group_id: string | null;
};

type GroupMember = {
  id: string;
  fcm_token: string | null;
};

Deno.serve(async (req) => {
  if (req.method !== "POST") {
    return new Response("Method not allowed", { status: 405 });
  }

  if (cronSecret) {
    const provided = req.headers.get("x-cron-secret");
    if (provided !== cronSecret) {
      console.error("notify-birthdays: missing or invalid X-Cron-Secret");
      return new Response("Forbidden", { status: 403 });
    }
  }

  const { mm, dd, ymd } = manilaToday();
  console.log(`notify-birthdays: scanning for MM-DD=${mm}-${dd} (PHT ${ymd})`);

  const supabase = createClient(supabaseUrl, serviceKey, {
    auth: { persistSession: false },
  });

  if (await isAlreadyProcessed(supabase, `notify-birthdays:${ymd}`)) {
    console.log(`notify-birthdays: day=${ymd} already processed — skip`);
    return ackSkipped("already-processed");
  }

  const { data: birthdayPeople, error: bdErr } = await supabase
    .rpc("birthdays_today_ph", { p_month: mm, p_day: dd });

  if (bdErr) {
    console.error(`notify-birthdays: birthdays_today_ph failed: ${bdErr.message}`);
    return new Response(`birthdays query failed: ${bdErr.message}`, { status: 500 });
  }

  const birthdays = (birthdayPeople ?? []) as BirthdayUser[];
  console.log(`notify-birthdays: matched ${birthdays.length} birthday user(s)`);

  if (birthdays.length === 0) {
    return new Response(
      JSON.stringify({ matched: 0, personal_sent: 0, cell_sent: 0 }),
      { status: 200, headers: { "Content-Type": "application/json" } },
    );
  }

  let personalSent = 0;
  let cellSent = 0;

  for (const person of birthdays) {
    if (person.fcm_token) {
      const ok = await sendFcm({
        token: person.fcm_token,
        title: "🎂 Happy Birthday!",
        body: `Happy birthday, ${person.name}! May your day be blessed.`,
        data: {
          channel: "community",
          destination: "home",
          id: person.id,
        },
      });
      if (ok) personalSent++;
    }

    if (!person.group_id) {
      console.log(`notify-birthdays: ${person.name} has no group_id — skip shoutout`);
      continue;
    }

    const { data: cell, error: cellErr } = await supabase
      .from("users")
      .select("id, fcm_token")
      .eq("group_id", person.group_id)
      .neq("id", person.id)
      .not("fcm_token", "is", null);

    if (cellErr) {
      console.error(
        `notify-birthdays: cell-members query failed for group=${person.group_id}: ${cellErr.message}`,
      );
      continue;
    }

    const tokens = Array.from(new Set(
      ((cell ?? []) as GroupMember[])
        .map((m) => m.fcm_token)
        .filter((t): t is string => !!t && t.length > 0),
    ));

    if (tokens.length === 0) continue;

    const results = await Promise.all(
      tokens.map((token) =>
        sendFcm({
          token,
          title: `🎂 It's ${person.name}'s birthday!`,
          body: "Take a moment to greet them today.",
          data: {
            channel: "community",
            destination: "home",
            id: person.id,
          },
        })
      ),
    );
    cellSent += results.filter(Boolean).length;
  }

  console.log(
    `notify-birthdays: done — matched=${birthdays.length} personal=${personalSent} cell=${cellSent}`,
  );
  return new Response(
    JSON.stringify({
      matched: birthdays.length,
      personal_sent: personalSent,
      cell_sent: cellSent,
    }),
    { status: 200, headers: { "Content-Type": "application/json" } },
  );
});
