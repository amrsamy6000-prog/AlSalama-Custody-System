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

## 2) نشر Worker على Cloudflare
1. افتح https://dash.cloudflare.com وسجّل الدخول.
2. من Workers & Pages أنشئ Worker باسم alsalama-auth.
3. افتح Edit code واستبدل المحتوى بمحتوى worker.js الموجود في هذا المجلد، ثم احفظ وانشر.
4. من Settings ثم Variables and Secrets أضف Secret باسم FIREBASE_SERVICE_ACCOUNT_JSON والصق محتوى ملف JSON كاملًا.
5. أضف Secret باسم EMPLOYEE_CODES_JSON. قيمته كائن JSON، ومثاله بالشكل التالي فقط (استبدل القيم بأكوادك الفعلية ولا تضعها في GitHub):
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
     }
   }
   ملاحظة: استبدل CODE_FOR_... بالأكواد الرقمية الفعلية كنصوص بين علامتي اقتباس.
6. أضف Rate Limiting binding باسم LOGIN_RATE_LIMITER، بحد 10 طلبات لكل 60 ثانية لكل عنوان IP. إعداد هذا الربط موجود في wrangler.toml عند النشر باستخدام Wrangler. إذا أنشأت Worker من لوحة التحكم فقط، أضف الربط من إعدادات Worker إن كان الخيار متاحًا.

## 3) اختبار الخدمة
افتح رابط Worker متبوعًا بـ /health. يجب أن يظهر:
{"ok":true,"service":"alsalama-auth"}

لا تختبر /login قبل إضافة السرّين. لا ترسل الأكواد أو ملف Service Account هنا.

## مهم قبل تشغيل النسخة الجديدة
- لا تغيّر قواعد Firestore الحالية الآن.
- لا توقف Anonymous Authentication الآن.
- لا تنشر APK جديدًا الآن.
- بعد نشر الخدمة وإرسال رابطها فقط، نربط شاشة الدخول بها ونحوّل Firebase إلى Custom Authentication، ثم نختبر جهازًا واحدًا.
- بعد نجاح الاختبار نغيّر قواعد Firestore لتقبل UIDs الأربعة فقط. بهذه الطريقة لا يستطيع عميل مجهول أو غريب القراءة أو الإضافة.
