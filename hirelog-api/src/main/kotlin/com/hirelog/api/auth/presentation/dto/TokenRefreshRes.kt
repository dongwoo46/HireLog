package com.hirelog.api.auth.presentation.dto

/**
 * Token Refresh 응답
 */
data class TokenRefreshRes(
    val accessToken: String,
    val refreshToken: String
)
