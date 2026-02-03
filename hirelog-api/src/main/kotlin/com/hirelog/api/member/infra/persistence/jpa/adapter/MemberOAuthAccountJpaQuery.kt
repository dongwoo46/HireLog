package com.hirelog.api.member.infra.persistence.jpa.adapter

import com.hirelog.api.auth.domain.OAuth2Provider
import com.hirelog.api.member.application.port.MemberOAuthAccountQuery
import com.hirelog.api.member.domain.MemberOAuthAccount
import com.hirelog.api.member.infra.persistence.jpa.repository.MemberOAuthAccountJpaRepository
import org.springframework.stereotype.Component

/**
 * MemberOAuthAccount JPA Query Adapter
 *
 * 책임:
 * - MemberOAuthAccountQuery Port의 JPA 구현
 * - 조회 전용
 */
@Component
class MemberOAuthAccountJpaQuery(
    private val repository: MemberOAuthAccountJpaRepository,
) : MemberOAuthAccountQuery {

    override fun findByProviderAndProviderUserId(
        provider: OAuth2Provider,
        providerUserId: String,
    ): MemberOAuthAccount? =
        repository.findByProviderAndProviderUserId(provider, providerUserId)
}
