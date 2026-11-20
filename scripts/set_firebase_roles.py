import firebase_admin
from firebase_admin import auth

firebase_admin.initialize_app(options={"projectId": "salama-custody"})

accounts = [
    ("Y58PeoAQJ6XGfkd6lM0nqAnVPJt2", "admin", "عمرو سامي"),
    ("InuQG0CrJraVEdyCWTAm6rAO4qI2", "finance_manager", "سامي هنداوي"),
    ("27IgBLSbcMcTqxYpS5MNUDVTp4X2", "operations", "عبدالله"),
    ("SsQfQmYmLfdsY3OS3U3wEVh5WgT2", "expense_user", "أحمد حمدي"),
]

for uid, role, name in accounts:
    user = auth.get_user(uid)
    claims = dict(user.custom_claims or {})
    claims.update({"role": role, "active": True})
    auth.set_custom_user_claims(uid, claims)
    print(f"تم ضبط صلاحيات {name}: {role}")

print("اكتمل ضبط صلاحيات الحسابات الأربعة.")
