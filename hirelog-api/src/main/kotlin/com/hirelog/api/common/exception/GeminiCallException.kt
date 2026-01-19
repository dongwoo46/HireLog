package com.hirelog.api.common.exception

/**
 * Gemini API 호출 실패 예외
 *
 * 의미:
 * - 네트워크 오류
 * - 타임아웃
 * - Gemini 서버 오류
 *
 * 사용 원칙:
 * - 사용자 메시지 ❌
 * - 로그/모니터링 전용
 * - HTTP 레이어에서는 502 Bad Gateway로 매핑
 */
class GeminiCallException(
    cause: Throwable
) : RuntimeException("Failed to call Gemini API", cause)
