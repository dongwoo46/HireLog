package com.hirelog.api.auth.domain

import com.hirelog.api.member.domain.Member

sealed class OAuth2LoginResult {
    /**
     * 기존 회원 로그인 성공
     *
     * - userId ❌
     * - Member Aggregate ✅
     */
    data class ExistingUser(
        val member: Member
    ) : OAuth2LoginResult()

    data class NewUser(
        val email: String?,
        val provider: OAuth2Provider,
        val providerUserId: String
    ) : OAuth2LoginResult()
}
