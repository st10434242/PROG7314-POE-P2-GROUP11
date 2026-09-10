package com.example.runway.data.auth

sealed interface SignInOutcome {

    data class Success(val session: AuthSession) : SignInOutcome

    sealed interface Failure : SignInOutcome {
        data object Cancelled : Failure
        data object NoAccount : Failure
        data object Configuration : Failure
        data object Network : Failure
        data object IncompleteAccount : Failure
        data object Failed : Failure
    }
}
