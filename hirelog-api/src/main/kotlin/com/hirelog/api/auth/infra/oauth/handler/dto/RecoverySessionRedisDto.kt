package com.hirelog.api.auth.infra.oauth.handler.dto

import com.hirelog.api.auth.domain.OAuth2Provider

data class RecoverySessionRedisDto(
    val memberId: Long,
    val provider: OAuth2Provider,
    val providerUserId: String
)
