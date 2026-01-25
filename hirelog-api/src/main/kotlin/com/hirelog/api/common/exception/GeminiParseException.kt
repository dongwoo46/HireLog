package com.hirelog.api.common.exception

/**
 * Gemini 응답 파싱 실패 예외
 *
 * 의미:
 * - JSON 파싱 실패
 * - 필수 필드 누락
 *
 * 사용 원칙:
 * - Worker 컨텍스트에서 발생 (REST 경로 아님)
 * - Facade에서 catch → Processing 상태 LLM_PARSE_FAILED로 기록
 * - RedisStreamConsumer에서 로깅 후 ACK 미수행
 */
class GeminiParseException(
    cause: Throwable
) : RuntimeException("Failed to parse Gemini response", cause)
