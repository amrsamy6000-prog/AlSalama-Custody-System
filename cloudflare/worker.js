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

    // Reports configuration readiness without exposing secret values.
    if (url.pathname === "/status" && request.method === "GET") {
      let employeesConfigured = false;
      let serviceAccountJsonPresent = false;
      let serviceAccountJsonValid = false;
      let hasClientEmail = false;
      let hasPrivateKey = false;
      let hasProjectId = false;
      let privateKeyLooksPem = false;
      try {
        const accounts = JSON.parse(env.EMPLOYEE_CODES_JSON || "null");
        employeesConfigured = !!accounts && typeof accounts === "object" && !Array.isArray(accounts) && Object.keys(accounts).length > 0;
      } catch {}
      try {
        const rawServiceAccount = env.FIREBASE_SERVICE_ACCOUNT_JSON || "";
        serviceAccountJsonPresent = rawServiceAccount.trim().length > 0;
        const serviceAccount = JSON.parse(rawServiceAccount || "null");
        serviceAccountJsonValid = !!serviceAccount && typeof serviceAccount === "object" && !Array.isArray(serviceAccount);
        hasClientEmail = serviceAccountJsonValid && typeof serviceAccount.client_email === "string" && serviceAccount.client_email.length > 0;
        hasPrivateKey = serviceAccountJsonValid && typeof serviceAccount.private_key === "string" && serviceAccount.private_key.length > 0;
        hasProjectId = serviceAccountJsonValid && typeof serviceAccount.project_id === "string" && serviceAccount.project_id.length > 0;
        privateKeyLooksPem = hasPrivateKey && serviceAccount.private_key.includes("-----BEGIN PRIVATE KEY-----") && serviceAccount.private_key.includes("-----END PRIVATE KEY-----");
      } catch {}
      const serviceAccountConfigured = serviceAccountJsonValid && hasClientEmail && hasPrivateKey && hasProjectId;
      return json({
        ok: employeesConfigured && serviceAccountConfigured,
        service: "alsalama-auth",
        checks: {
          employeeCodesConfigured: employeesConfigured,
          firebaseServiceAccountConfigured: serviceAccountConfigured,
          serviceAccountJsonPresent,
          serviceAccountJsonValid,
          hasClientEmail,
          hasPrivateKey,
          hasProjectId,
          privateKeyLooksPem
        }
      });
    }

    // Simple same-origin login tester. The token is never rendered or logged.
    if (url.pathname === "/test" && request.method === "GET") {
      const page = `<!doctype html>
<html lang="ar" dir="rtl"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><meta name="theme-color" content="#123c32"><title>اختبار دخول العهدة</title>
<style>
*{box-sizing:border-box}body{margin:0;min-height:100vh;display:grid;place-items:center;padding:20px;background:linear-gradient(145deg,#0d2824,#174b3e);font-family:Tahoma,Arial,sans-serif;color:#172b27}.card{width:min(100%,420px);background:#fff;border-radius:22px;padding:26px;box-shadow:0 18px 55px #0004}.mark{width:54px;height:54px;border-radius:16px;background:#e7f3ed;color:#176344;display:grid;place-items:center;font-size:27px;margin-bottom:16px}h1{font-size:22px;margin:0 0 8px}p{color:#66736e;font-size:14px;line-height:1.7;margin:0 0 22px}label{display:block;font-weight:bold;font-size:14px;margin-bottom:9px}input{width:100%;height:54px;border:1px solid #ccd8d2;border-radius:12px;padding:0 15px;font-size:22px;letter-spacing:5px;text-align:center;outline:none}input:focus{border-color:#23845d;box-shadow:0 0 0 3px #23845d22}button{width:100%;height:52px;margin-top:16px;border:0;border-radius:12px;background:#176344;color:#fff;font-size:16px;font-weight:bold;cursor:pointer}button:disabled{opacity:.6}.result{display:none;margin-top:16px;padding:13px;border-radius:12px;font-size:14px;line-height:1.7;overflow-wrap:anywhere}.ok{display:block;background:#e8f6ed;color:#176344}.err{display:block;background:#fff0ee;color:#a12d25}.hint{font-size:12px;text-align:center;margin:17px 0 0;color:#87918c}
</style></head><body><main class="card"><div class="mark">✓</div><h1>اختبار تسجيل الدخول</h1><p>أدخل كود الموظف لاختبار الاتصال بخدمة العهدة. لن يتم عرض رمز الدخول السري على الصفحة.</p><form id="form"><label for="code">كود الموظف</label><input id="code" inputmode="numeric" autocomplete="off" maxlength="6" minlength="4" pattern="[0-9]{4,6}" placeholder="••••" required><button id="submit" type="submit">اختبار الدخول</button></form><div id="result" class="result" role="status" aria-live="polite"></div><div class="hint">مصنع السلامة · خدمة التحقق الآمن</div></main>
<script>
const form=document.getElementById('form'),code=document.getElementById('code'),btn=document.getElementById('submit'),result=document.getElementById('result');
form.addEventListener('submit',async e=>{e.preventDefault();const value=code.value.trim();if(!/^[0-9]{4,6}$/.test(value)){result.className='result err';result.textContent='الكود يجب أن يتكون من 4 إلى 6 أرقام.';return}btn.disabled=true;btn.textContent='جارٍ الاختبار...';result.className='result';result.textContent='';try{const response=await fetch('/login',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({code:value})});const data=await response.json();if(response.ok&&data.customToken){result.className='result ok';result.textContent='تم التحقق بنجاح ✓ مرحبًا '+(data.employeeName||'بك')+' — خدمة الدخول تعمل بشكل صحيح.';code.value='';}else{const messages={invalid_code:'كود الموظف غير صحيح أو غير مسجل.',invalid_request:'بيانات الطلب غير مكتملة.',server_configuration_error:'يوجد خطأ في إعدادات الخدمة.',token_creation_failed:'تعذر إنشاء رمز الدخول؛ راجع إعدادات Firebase.',too_many_attempts:'محاولات كثيرة. انتظر قليلًا ثم حاول مرة أخرى.'};result.className='result err';result.textContent=messages[data.error]||('فشل الاختبار (HTTP '+response.status+').');}}catch(err){result.className='result err';result.textContent='تعذر الاتصال بالخدمة. تحقق من الإنترنت ثم أعد المحاولة.';}finally{btn.disabled=false;btn.textContent='اختبار الدخول';}});
</script></body></html>`;
      return new Response(page, { headers: { "content-type": "text/html; charset=utf-8", "cache-control": "no-store" } });
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
