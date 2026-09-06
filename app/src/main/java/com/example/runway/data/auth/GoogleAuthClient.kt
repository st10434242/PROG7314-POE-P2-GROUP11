package com.example.runway.data.auth

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.Task

/** Wraps the Google Sign-In client so the fragments stay focused on UI. */
class GoogleAuthClient(context: Context) {

    private val appContext = context.applicationContext

    fun buildSignInIntent(emailHint: String? = null): Intent {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .apply {
                emailHint?.takeIf { it.isNotBlank() }?.let { setAccountName(it) }
            }
            .build()

        return GoogleSignIn.getClient(appContext, options).signInIntent
    }

    fun signOut(): Task<Void> = GoogleSignIn.getClient(
        appContext,
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build(),
    ).signOut()

}
