package com.hirelog.api.member.application

import com.hirelog.api.common.config.properties.AdminProperties
import com.hirelog.api.member.domain.policy.UsernameValidationPolicies
import com.hirelog.api.member.domain.policy.UsernameValidationPolicy
import org.springframework.stereotype.Component

/**
 * Username 검증 정책 결정자 (Application)
 */
@Component
class UsernameValidationPolicyResolver(
    private val adminProperties: AdminProperties
) {

    fun resolve(email: String): UsernameValidationPolicy {
        return if (adminProperties.isAdmin(email)) {
            UsernameValidationPolicies.BYPASS
        } else {
            UsernameValidationPolicies.STRICT
        }
    }
}
