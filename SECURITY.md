
# Security Policy

## Important Rules

- Never upload passwords or employee login credentials.
- Never upload Firebase Service Account private keys.
- Never upload real financial or personal data.
- Never publish Firestore rules that allow unrestricted access.
- Never trust client-side role checks alone.
- All permissions must be enforced by secure database rules or a trusted server.
- Financial records must not be permanently deleted.
- Corrections must be recorded as audited reversal entries.

## Firebase

The google-services.json file must remain local and must not be committed to this public repository.

Firestore security rules must be tested with all user roles before production use.

## Reporting Security Issues

Do not publish sensitive vulnerability details in public issues.
