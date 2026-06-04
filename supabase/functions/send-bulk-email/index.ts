// Admin-only bulk email sender. Audience options:
//   - all                       → every member with email
//   - role(s) only              → only the named roles
//   - group only                → only members of one cell group
// Auth model: the function reads the caller's JWT (forwarded by supabase-js
// when invoke() is called from a signed-in client), looks up their role in
// the users table, and rejects anyone who isn't youth_president/pastor/admin.
// Service role is used internally for the actual user query so RLS doesn't
// block the audience lookup.

import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

type Audience =
  | { kind: "all" }
  | { kind: "roles"; roles: string[] }
  | { kind: "group"; group_id: string };

type Body = {
  subject: string;
  message: string;
  audience: Audience;
};

const supabaseUrl = Deno.env.get("SUPABASE_URL")!;
const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
const resendKey = Deno.env.get("RESEND_API_KEY")!;
// e.g. "GRACE Youth <ministry@yourchurch.org>" — must be a verified Resend sender.
const fromEmail = Deno.env.get("BULK_EMAIL_FROM") ?? "GRACE <noreply@grace.app>";

const SENIOR_ROLES = new Set(["youth_president", "pastor", "admin"]);

function escapeHtml(s: string): string {
  return s
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#039;");
}

function htmlBody(message: string): string {
  // Preserve paragraph breaks — Resend supports raw HTML.
  const paragraphs = message
    .split(/\n{2,}/)
    .map((p) => `<p style="margin:0 0 12px 0; line-height:1.5">${escapeHtml(p).replace(/\n/g, "<br/>")}</p>`)
    .join("");
  return `<!doctype html><html><body style="font-family:Lato,Arial,sans-serif;color:#1a1a1a;background:#f4f1ea;padding:24px;">
    <div style="max-width:560px;margin:0 auto;background:#fff;border-radius:12px;padding:28px;border:1px solid #e6decf">
      <div style="font-family:'Cormorant Garamond',serif;color:#c9a84c;font-size:28px;font-weight:600;margin-bottom:6px">THE KINGDOM BUILDERS</div>
      <hr style="border:0;border-top:1px solid #eee;margin:14px 0"/>
      ${paragraphs}
      <hr style="border:0;border-top:1px solid #eee;margin:18px 0"/>
      <div style="font-size:11px;color:#888">You're receiving this because you're part of The Kingdom Builders community.</div>
    </div>
  </body></html>`;
}

async function getCallerRole(authHeader: string | null): Promise<string | null> {
  if (!authHeader) return null;
  // Validate JWT by asking Supabase Auth — handles signature + expiry.
  const userClient = createClient(supabaseUrl, serviceKey, {
    global: { headers: { Authorization: authHeader } },
    auth: { persistSession: false },
  });
  const { data, error } = await userClient.auth.getUser();
  if (error || !data.user) return null;
  const svc = createClient(supabaseUrl, serviceKey, { auth: { persistSession: false } });
  const { data: row } = await svc
    .from("users").select("role").eq("id", data.user.id).maybeSingle();
  return (row?.role as string | undefined) ?? null;
}

async function resolveRecipients(audience: Audience): Promise<{ email: string; name: string }[]> {
  const svc = createClient(supabaseUrl, serviceKey, { auth: { persistSession: false } });
  let query = svc.from("users").select("email, name").not("email", "is", null);

  if (audience.kind === "roles") {
    if (audience.roles.length === 0) return [];
    query = query.in("role", audience.roles);
  } else if (audience.kind === "group") {
    query = query.eq("group_id", audience.group_id);
  }
  const { data, error } = await query;
  if (error) throw new Error(error.message);
  return (data ?? []).filter((r) => !!r.email);
}

async function sendViaResend(to: string, subject: string, html: string): Promise<boolean> {
  try {
    const res = await fetch("https://api.resend.com/emails", {
      method: "POST",
      headers: {
        "Authorization": `Bearer ${resendKey}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        from: fromEmail,
        to: [to],
        subject,
        html,
      }),
    });
    return res.ok;
  } catch {
    return false;
  }
}

Deno.serve(async (req) => {
  if (req.method !== "POST") return new Response("Method not allowed", { status: 405 });

  const role = await getCallerRole(req.headers.get("Authorization"));
  if (!role || !SENIOR_ROLES.has(role)) {
    return new Response("Forbidden — leader role required", { status: 403 });
  }

  let body: Body;
  try {
    body = await req.json();
  } catch {
    return new Response("Invalid JSON", { status: 400 });
  }
  if (!body.subject?.trim() || !body.message?.trim()) {
    return new Response("subject and message are required", { status: 400 });
  }

  let recipients;
  try {
    recipients = await resolveRecipients(body.audience);
  } catch (e) {
    return new Response(`recipients query failed: ${(e as Error).message}`, { status: 500 });
  }

  if (recipients.length === 0) {
    return new Response(
      JSON.stringify({ recipients: 0, sent: 0 }),
      { status: 200, headers: { "Content-Type": "application/json" } },
    );
  }

  const html = htmlBody(body.message);
  // Resend has per-request rate limits — send in series of small batches.
  // For typical youth-ministry sizes (< 200 members) this is plenty fast.
  let sent = 0;
  for (const r of recipients) {
    const ok = await sendViaResend(r.email, body.subject, html);
    if (ok) sent++;
  }

  return new Response(
    JSON.stringify({ recipients: recipients.length, sent }),
    { status: 200, headers: { "Content-Type": "application/json" } },
  );
});
