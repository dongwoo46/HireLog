package com.hirelog.api.auth.domain

sealed class OAuth2LoginResult {
    /**
     * 기존 회원 로그인 성공
     */
    data class ExistingUser(
        val memberId: Long,
        val role: String,
    ) : OAuth2LoginResult()

    data class NewUser(
        val email: String?,
        val provider: OAuth2Provider,
        val providerUserId: String
    ) : OAuth2LoginResult()
}
