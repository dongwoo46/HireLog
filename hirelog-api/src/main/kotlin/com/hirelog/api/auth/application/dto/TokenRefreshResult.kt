package com.hirelog.api.auth.application.dto

data class TokenRefreshResult(
    val accessToken: String,
    val refreshToken: String,
    val memberId: Long
)