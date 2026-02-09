package com.hirelog.api.common.exception

/**
 * 인증 코드 만료 예외
 *
 * 발생 시점:
 * - Redis에 인증 코드가 존재하지 않음
 * - TTL 만료
 *
 * 의미:
 * - 사용자가 입력한 코드가 더 이상 유효하지 않음
 */
class VerificationCodeExpiredException : RuntimeException(
    "Verification code has expired."
)
