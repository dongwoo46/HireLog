package com.hirelog.api.common.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "hirelog.admin")
class AdminProperties {

    /**
     * ADMIN 권한을 부여할 이메일 목록
     *
     * 예:
     * hirelog.admin.emails=siwol406@gmail.com,admin@hirelog.io
     */
    var emails: String = ""

    private val emailSet: Set<String> by lazy {
        emails
            .split(",")
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() }
            .toSet()
    }

    fun isAdmin(email: String): Boolean {
        return email.lowercase() in emailSet
    }
}
