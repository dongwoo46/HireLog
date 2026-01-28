package com.hirelog.api.member.application.port

import com.hirelog.api.auth.domain.OAuth2Provider
import com.hirelog.api.member.domain.MemberOAuthAccount

/**
 * MemberOAuthAccount Read Port
 *
 * 책임:
 * - OAuth 계정 연결 조회 계약 정의
 */
interface MemberOAuthAccountQuery {

    fun findByProviderAndProviderUserId(
        provider: OAuth2Provider,
        providerUserId: String,
    ): MemberOAuthAccount?
}
