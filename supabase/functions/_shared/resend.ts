
type SendArgs = {
  to: string;
  subject: string;
  html: string;
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

export function escapeHtml(s: string): string {
  return s
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#039;");
}
