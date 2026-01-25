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
 * - Worker 컨텍스트에서 발생 (REST 경로 아님)
 * - Facade에서 catch → Processing 상태 LLM_CALL_FAILED로 기록
 * - RedisStreamConsumer에서 로깅 후 ACK 미수행
 */
class GeminiCallException(
    cause: Throwable
) : RuntimeException("Failed to call Gemini API", cause)
