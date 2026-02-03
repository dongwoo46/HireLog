package com.hirelog.api.auth.application.dto

data class AuthTokens(
    val accessToken: String,
    val refreshToken: String
)