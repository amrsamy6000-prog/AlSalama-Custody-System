/**
 * AlSalama Custody - Firebase Custom Token issuer.
 *
 * Secrets configured in Cloudflare (never commit them):
 *   EMPLOYEE_CODES_JSON
 *   FIREBASE_SERVICE_ACCOUNT_JSON
 *
 * Login is intentionally code-based. The employee code is checked here,
 * outside the APK, and a Firebase custom token is returned only on success.
 */
const JSON_HEADERS = {
  "content-type": "application/json; charset=utf-8",
  "cache-control": "no-store, no-cache, must-revalidate",
  "pragma": "no-cache"
};

function json(data, status = 200) {
  return new Response(JSON.stringify(data), { status, headers: JSON_HEADERS });
}

function base64Url(bytes) {
  let binary = "";
  for (const byte of new Uint8Array(bytes)) binary += String.fromCharCode(byte);
  return btoa(binary).replace(/=/g, "").replace(/\+/g, "-").replace(/\//g, "_");
}

function encodeJson(value) {
  return base64Url(new TextEncoder().encode(JSON.stringify(value)));
}

function pemToBytes(pem) {
  const body = pem
    .replace("-----BEGIN PRIVATE KEY-----", "")
    .replace("-----END PRIVATE KEY-----", "")
    .replace(/\s/g, "");
  const binary = atob(body);
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i);
  return bytes;
}

async function createFirebaseCustomToken(serviceAccount, account) {
  const now = Math.floor(Date.now() / 1000);
  const header = {
    alg: "RS256",
    typ: "JWT",
    ...(serviceAccount.private_key_id ? { kid: serviceAccount.private_key_id } : {})
  };
  const payload = {
    iss: serviceAccount.client_email,
    sub: serviceAccount.client_email,
    aud: "https://identitytoolkit.googleapis.com/google.identity.identitytoolkit.v1.IdentityToolkit",
    iat: now,
    exp: now + 3600,
    uid: account.uid,
    claims: {
      app_access: true,
      employee_name: account.employeeName
    }
  };

  const unsigned = encodeJson(header) + "." + encodeJson(payload);
  const key = await crypto.subtle.importKey(
    "pkcs8",
    pemToBytes(serviceAccount.private_key),
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["sign"]
  );
  const signature = await crypto.subtle.sign(
    "RSASSA-PKCS1-v1_5",
    key,
    new TextEncoder().encode(unsigned)
  );
  return unsigned + "." + base64Url(signature);
}

export default {
  async fetch(request, env) {
    const url = new URL(request.url);

    if (url.pathname === "/health" && request.method === "GET") {
      return json({ ok: true, service: "alsalama-auth" });
    }

    if (url.pathname !== "/login" || request.method !== "POST") {
      return json({ error: "not_found" }, 404);
    }

    // Approximate per-IP throttling. The configured limit allows a small
    // group of employees to sign in around the same time.
    if (env.LOGIN_RATE_LIMITER) {
      const ip = request.headers.get("CF-Connecting-IP") || "unknown";
      const limit = await env.LOGIN_RATE_LIMITER.limit({ key: ip });
      if (!limit.success) return json({ error: "too_many_attempts" }, 429);
    }

    const length = Number(request.headers.get("content-length") || "0");
    if (length > 1024) return json({ error: "invalid_request" }, 413);

    let body;
    try {
      body = await request.json();
    } catch {
      return json({ error: "invalid_request" }, 400);
    }

    const code = typeof body?.code === "string" ? body.code.trim() : "";
    if (!/^\d{4,6}$/.test(code)) {
      return json({ error: "invalid_code" }, 401);
    }

    let accounts;
    let serviceAccount;
    try {
      accounts = JSON.parse(env.EMPLOYEE_CODES_JSON || "{}");
      serviceAccount = JSON.parse(env.FIREBASE_SERVICE_ACCOUNT_JSON || "{}");
    } catch {
      return json({ error: "server_configuration_error" }, 500);
    }

    const account = accounts[code];
    if (
      !account ||
      typeof account.uid !== "string" ||
      !/^[a-zA-Z0-9_-]{1,128}$/.test(account.uid) ||
      typeof account.employeeName !== "string" ||
      !serviceAccount.client_email ||
      !serviceAccount.private_key ||
      !serviceAccount.project_id
    ) {
      return json({ error: account ? "server_configuration_error" : "invalid_code" }, account ? 500 : 401);
    }

    try {
      const customToken = await createFirebaseCustomToken(serviceAccount, account);
      return json({
        customToken,
        uid: account.uid,
        employeeName: account.employeeName,
        employeeCode: code
      });
    } catch {
      // Never log credentials, private keys, or submitted employee codes.
      return json({ error: "token_creation_failed" }, 500);
    }
  }
};
