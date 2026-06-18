
type ServiceAccount = {
  project_id: string;
  client_email: string;
  private_key: string;
};

type FcmMessage = {
  token: string;
  title: string;
  body: string;
  data: Record<string, string>;
};

let cachedToken: { value: string; expiresAt: number } | null = null;

function loadServiceAccount(): ServiceAccount {
  const raw = Deno.env.get("FCM_SERVICE_ACCOUNT_JSON");
  if (!raw) {
    throw new Error("FCM_SERVICE_ACCOUNT_JSON is not configured");
  }
  return JSON.parse(raw);
}

function base64UrlFromBytes(bytes: Uint8Array): string {
  return btoa(String.fromCharCode(...bytes))
    .replace(/=+$/g, "")
    .replace(/\+/g, "-")
    .replace(/\//g, "_");
}

function base64UrlFromString(s: string): string {
  return base64UrlFromBytes(new TextEncoder().encode(s));
}

async function importPrivateKey(pem: string): Promise<CryptoKey> {
  const body = pem
    .replace(/-----BEGIN PRIVATE KEY-----/g, "")
    .replace(/-----END PRIVATE KEY-----/g, "")
    .replace(/\s+/g, "");
  const binary = Uint8Array.from(atob(body), (c) => c.charCodeAt(0));
  return await crypto.subtle.importKey(
    "pkcs8",
    binary,
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["sign"],
  );
}

async function fetchAccessToken(): Promise<{ token: string; projectId: string }> {
  const sa = loadServiceAccount();
  const now = Math.floor(Date.now() / 1000);
  if (cachedToken && cachedToken.expiresAt > now + 60) {
    return { token: cachedToken.value, projectId: sa.project_id };
  }

  const header = base64UrlFromString(
    JSON.stringify({ alg: "RS256", typ: "JWT" }),
  );
  const payload = base64UrlFromString(JSON.stringify({
    iss: sa.client_email,
    scope: "https://www.googleapis.com/auth/firebase.messaging",
    aud: "https://oauth2.googleapis.com/token",
    exp: now + 3600,
    iat: now,
  }));
  const unsigned = `${header}.${payload}`;
  const key = await importPrivateKey(sa.private_key);
  const sigBytes = new Uint8Array(
    await crypto.subtle.sign("RSASSA-PKCS1-v1_5", key, new TextEncoder().encode(unsigned)),
  );
  const jwt = `${unsigned}.${base64UrlFromBytes(sigBytes)}`;

  const res = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion: jwt,
    }),
  });
  if (!res.ok) {
    throw new Error(`OAuth exchange failed: ${res.status} ${await res.text()}`);
  }
  const json = await res.json();
  cachedToken = {
    value: json.access_token,
    expiresAt: now + (json.expires_in ?? 3600),
  };
  return { token: cachedToken.value, projectId: sa.project_id };
}

export async function sendFcm(msg: FcmMessage): Promise<boolean> {
  const tokenHint = msg.token.slice(0, 12) + "…";
  try {
    const { token, projectId } = await fetchAccessToken();
    const res = await fetch(
      `https://fcm.googleapis.com/v1/projects/${projectId}/messages:send`,
      {
        method: "POST",
        headers: {
          "Authorization": `Bearer ${token}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          message: {
            token: msg.token,
            notification: { title: msg.title, body: msg.body },
            data: msg.data,
            android: { priority: "HIGH" },
          },
        }),
      },
    );
    if (!res.ok) {
      const errBody = await res.text();
      console.error(
        `FCM send failed for ${tokenHint}: HTTP ${res.status} ${errBody}`,
      );
      return false;
    }
    console.log(`FCM send ok for ${tokenHint}`);
    return true;
  } catch (e) {
    console.error(`FCM send threw for ${tokenHint}:`, e);
    return false;
  }
}
