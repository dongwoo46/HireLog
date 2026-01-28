package com.hirelog.api.member.infra.persistence.jpa

import com.hirelog.api.auth.domain.OAuth2Provider
import com.hirelog.api.member.domain.MemberOAuthAccount
import org.springframework.data.jpa.repository.JpaRepository

interface MemberOAuthAccountJpaRepository : JpaRepository<MemberOAuthAccount, Long> {

    fun findByProviderAndProviderUserId(
        provider: OAuth2Provider,
        providerUserId: String,
    ): MemberOAuthAccount?
}
