package com.hirelog.api.auth.domain

sealed class OAuth2LoginResult {
    data class ExistingUser(val userId: Long) : OAuth2LoginResult()
    data class NewUser(
        val email: String?,
        val provider: OAuth2Provider,
        val providerUserId: String
    ) : OAuth2LoginResult()
}
