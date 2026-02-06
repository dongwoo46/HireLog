package com.hirelog.api.common.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "auth.frontend") // YAML의 app.auth.frontend와 매칭
data class OAuthFrontendProperties(
    val mainUrl: String,   // YAML의 main-url (스네이크 케이스를 카멜 케이스로 자동 변환)
    val signupUrl: String,  // YAML의 signup-url
    val recoveryUrl: String
)