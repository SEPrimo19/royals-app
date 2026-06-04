// Resend HTTP API helper. Used by send-bulk-email (broadcast) and
// welcome-on-checkin (personalized per attendee).
//
// Env vars (lazy, only read when sending — keeps imports safe in dev):
//   RESEND_API_KEY    — re_... key from resend.com/api-keys
//   BULK_EMAIL_FROM   — "GRACE Youth <youth@yourchurch.org>" or onboarding@resend.dev

type SendArgs = {
  to: string;
  subject: string;
  html: string;
  // Optional override for the "From" — defaults to BULK_EMAIL_FROM env. Useful
  // if a future feature wants a different sender per email type.
  from?: string;
};

export async function sendResendEmail(args: SendArgs): Promise<boolean> {
  const apiKey = Deno.env.get("RESEND_API_KEY");
  if (!apiKey) {
    console.error("sendResendEmail: RESEND_API_KEY is not configured");
    return false;
  }
  const from = args.from
    ?? Deno.env.get("BULK_EMAIL_FROM")
    ?? "GRACE <onboarding@resend.dev>";

  try {
    const res = await fetch("https://api.resend.com/emails", {
      method: "POST",
      headers: {
        "Authorization": `Bearer ${apiKey}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        from,
        to: [args.to],
        subject: args.subject,
        html: args.html,
      }),
    });
    if (!res.ok) {
      const body = await res.text();
      console.error(
        `Resend send failed for ${args.to}: HTTP ${res.status} ${body}`,
      );
      return false;
    }
    return true;
  } catch (e) {
    console.error(`Resend send threw for ${args.to}:`, e);
    return false;
  }
}

/** HTML escape — keep this in sync with send-bulk-email's local copy. */
export function escapeHtml(s: string): string {
  return s
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#039;");
}
