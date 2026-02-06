package com.hirelog.api.common.exception

/**
 * 정지(SUSPENDED)된 회원 접근 시 발생하는 예외
 *
 * 의미:
 * - OAuth 인증은 성공했으나
 * - 계정 상태 정책상 서비스 이용이 불가능한 상태
 *
 * 사용 위치:
 * - OAuth 로그인 처리
 * - 인증 이후 사용자 식별 단계
 *
 * 주의:
 * - 자동 복구 ❌
 * - 재로그인으로 해결 ❌
 */
class SuspendedMemberException(
    override val message: String = "정지된 계정입니다. 고객센터에 문의해주세요."
) : RuntimeException(message)
