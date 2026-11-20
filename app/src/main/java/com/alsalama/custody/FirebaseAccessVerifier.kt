package com.alsalama.custody

import com.google.firebase.auth.FirebaseUser

internal object FirebaseAccessVerifier {
    fun verify(
        user: FirebaseUser,
        expectedUid: String,
        expectedRole: String,
        onResult: (Boolean) -> Unit
    ) {
        if (user.uid != expectedUid) {
            onResult(false)
            return
        }
        user.getIdToken(true).addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                onResult(false)
            } else {
                val claims = task.result?.claims
                onResult(
                    claims?.get("role") == expectedRole &&
                        claims["active"] == true
                )
            }
        }
    }
}
