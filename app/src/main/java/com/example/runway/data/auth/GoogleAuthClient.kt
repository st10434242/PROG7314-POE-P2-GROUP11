package com.example.runway.data.auth

import com.example.runway.R
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

// Google sign-in and the exchange for a Firebase identity (IIE, 2026; Sandoval, 2016).
class GoogleAuthClient(context: Context) {
    private val appContext = context.applicationContext

    private val firebaseAuth: FirebaseAuth get() = FirebaseAuth.getInstance()

    // requestIdToken is what asks Google for a token rather than just an email address.
    private fun options(emailHint: String? = null): GoogleSignInOptions =
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(appContext.getString(R.string.default_web_client_id))
            .requestEmail()
            .apply {
                emailHint?.takeIf { it.isNotBlank() }?.let { setAccountName(it) }
            }
            .build()

    fun buildSignInIntent(emailHint: String? = null): Intent =
        GoogleSignIn.getClient(appContext, options(emailHint)).signInIntent

    // Turns a Google ID token into a signed-in Firebase user.
    suspend fun signInToFirebase(
        googleIdToken: String,
        biometricEnabled: Boolean,
    ): AuthSession {
        val credential = GoogleAuthProvider.getCredential(googleIdToken, null)
        val result = firebaseAuth.signInWithCredential(credential).await()

        val user = requireNotNull(result.user) { "Firebase returned no user for this credential" }

        return AuthSession(
            id = user.uid,
            email = user.email.orEmpty(),
            displayName = user.displayName.orEmpty(),
            photoUrl = user.photoUrl?.toString(),
            signedInAtMillis = System.currentTimeMillis(),
            biometricEnabled = biometricEnabled,
        )
    }

    // Signs out of both.
    fun signOut(): Task<Void> {
        firebaseAuth.signOut()
        return GoogleSignIn.getClient(appContext, options()).signOut()
    }
}

/* Reference List
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
Sandoval, K., 2016. What is the Difference Between an API and an SDK?. [online] Available at: <https://nordicapis.com/what-is-the-difference-between-an-api-and-an-sdk/> [Accessed 31 July 2023].
*/
