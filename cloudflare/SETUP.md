# إعداد خدمة الدخول الآمن لتطبيق العُهدة

هذه الخدمة تتحقق من كود الموظف خارج ملف APK، ثم تصدر Firebase Custom Token.
لا ترفع أي أكواد موظفين أو مفاتيح Google إلى GitHub.

## ما الذي ستحتاجه؟
- حساب Cloudflare مجاني.
- صلاحية الدخول إلى مشروع Firebase الخاص بتطبيق العُهدة.
- ملف Service Account JSON من Firebase. هذا الملف سري جدًا.

## 1) إنشاء مفتاح حساب الخدمة من Firebase
1. افتح Firebase Console ثم Project settings.
2. افتح تبويب Service accounts.
3. اختر Generate new private key ونزّل ملف JSON.
4. احتفظ بالملف لنفسك. لا ترسله في المحادثات ولا ترفعه إلى GitHub.

المفتاح يُستخدم فقط لتوقيع Firebase Custom Tokens. توثيق Firebase:
https://firebase.google.com/docs/auth/admin/create-custom-tokens

## 2) النشر التلقائي من GitHub
تم تجهيز Worker داخل هذا المجلد:
- Root directory / Path: `cloudflare`
- Production branch: `setup/android-app-foundation`
- Build command: اتركه فارغًا
- Deploy command: `npx wrangler deploy`

بعد ربط GitHub، أي commit جديد على فرع الإنتاج يشغّل Cloudflare Builds تلقائيًا. تأكد من أن آخر Deployment مصدره GitHub وحالته Ready قبل اختبار الخدمة.

## 3) إضافة الأسرار في Cloudflare
من Worker ثم Settings ثم Variables and Secrets أضف:
1. Secret باسم `FIREBASE_SERVICE_ACCOUNT_JSON`، وقيمته محتوى ملف Service Account JSON كاملًا.
2. Secret باسم `EMPLOYEE_CODES_JSON`. قيمته كائن JSON، ومثاله بالشكل التالي فقط (استبدل القيم بأكوادك الفعلية ولا تضعها في GitHub):
   {
     "CODE_FOR_AMR": {
       "uid": "salama_amr_samy",
       "employeeName": "عمرو سامي"
     },
     "CODE_FOR_SAMY": {
       "uid": "salama_samy_hendawy",
       "employeeName": "سامي هنداوي"
     },
     "CODE_FOR_ABDULLAH": {
       "uid": "salama_abdullah",
       "employeeName": "عبدالله"
     },
     "CODE_FOR_AHMED": {
       "uid": "salama_ahmed_hamdy",
       "employeeName": "أحمد حمدي"
     },
     "CODE_FOR_AHMED_HASSAN": { "uid": "salama_ahmed_hassan", "employeeName": "أحمد حسن" },
     "CODE_FOR_AHMED_YASSER": { "uid": "salama_ahmed_yasser", "employeeName": "أحمد ياسر" },
     "CODE_FOR_MOHAMED_EL_SAYED": { "uid": "salama_mohamed_elsayed", "employeeName": "محمد السيد" },
     "CODE_FOR_USER1": { "uid": "salama_user1", "employeeName": "USER1" },
     "CODE_FOR_USER2": { "uid": "salama_user2", "employeeName": "USER2" },
     "CODE_FOR_USER3": { "uid": "salama_user3", "employeeName": "USER3" },
     "CODE_FOR_USER4": { "uid": "salama_user4", "employeeName": "USER4" }
   }
   ملاحظة: استبدل CODE_FOR_... بالأكواد الرقمية الفعلية كنصوص بين علامتي اقتباس. الخدمة تقبل أكوادًا رقمية من 4 إلى 6 أرقام، وتُحفظ كـ strings حتى لا يضيع الصفر في بداية الكود. أضف الحسابات الـ11 المعتمدة (الأربعة الحالية والسبعة الجديدة) إلى هذا السر؛ لا تضع الأكواد الحقيقية في GitHub أو المحادثة.
3. إعداد Rate Limiting binding باسم `LOGIN_RATE_LIMITER` بحد 10 طلبات لكل 60 ثانية لكل عنوان IP. تعريفه موجود في `wrangler.toml`.

## 4) اختبار الخدمة
افتح رابط Worker متبوعًا بـ `/health`. يجب أن يظهر:
`{"ok":true,"service":"alsalama-auth"}`

لا تختبر `/login` قبل إضافة السرّين. لا ترسل الأكواد أو ملف Service Account هنا.

## مهم قبل تشغيل النسخة الجديدة
- لا تغيّر قواعد Firestore الحالية الآن.
- لا توقف Anonymous Authentication الآن.
- لا تنشر APK جديدًا الآن.
- بعد نشر الخدمة وإضافة الأسرار، نربط شاشة الدخول بها ونحوّل Firebase إلى Custom Authentication، ثم نختبر جهازًا واحدًا.
- بعد نجاح الاختبار نغيّر قواعد Firestore لتقبل UIDs الموظفين فقط. بهذه الطريقة لا يستطيع عميل مجهول أو غريب القراءة أو الإضافة.
