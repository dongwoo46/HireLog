package com.hirelog.api.common.exception

/**
 * 인증 코드 불일치 예외
 *
 * 의미:
 * - 이메일 또는 인증 코드가 일치하지 않음
 * - 재입력으로 해결 가능한 가역 실패
 */
class InvalidVerificationCodeException(
    override val message: String = "인증 코드가 올바르지 않습니다."
) : RuntimeException(message)