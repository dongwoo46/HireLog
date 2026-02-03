package com.hirelog.api.member.application.dto

/**
 * 회원가입/연동 완료 결과
 */
data class SignupResult(
    val memberId: Long,
    val accessToken: String,
    val refreshToken: String
)