package com.hirelog.api.common.infra.redis.messaging

/**
 * Redis Stream Message Serializer
 *
 * 책임:
 * - 메시지를 Redis Stream에 적합한 Map<String, String> 형태로 변환
 * - Flat한 key-value 구조를 강제
 *
 * 비책임:
 * - 도메인 객체 생성 ❌
 * - Stream 발행 ❌
 * - 메시지 처리 ❌
 */
object RedisStreamSerializer {

    private const val PAYLOAD_PREFIX = "payload."

    /**
     * 메시지 메타 + payload를 Stream용 Map으로 직렬화
     *
     * @param metadata 메시지 메타 정보 (jobId, type 등)
     * @param payload  실제 처리 데이터
     */
    fun serialize(
        metadata: Map<String, String>,
        payload: Map<String, String>
    ): Map<String, String> {
        require(metadata.isNotEmpty()) { "metadata must not be empty" }

        val result = mutableMapOf<String, String>()

        // 메타 정보 그대로 복사
        result.putAll(metadata)

        // payload는 prefix를 붙여 평탄화
        payload.forEach { (key, value) ->
            result["$PAYLOAD_PREFIX$key"] = value
        }

        return result
    }

    /**
     * Stream 메시지를 메타 / payload로 분리
     *
     * @param message Redis Stream에서 읽은 메시지
     */
    fun deserialize(
        message: Map<String, String>
    ): DeserializedMessage {
        require(message.isNotEmpty()) { "message must not be empty" }

        val metadata = mutableMapOf<String, String>()
        val payload = mutableMapOf<String, String>()

        message.forEach { (key, value) ->
            if (key.startsWith(PAYLOAD_PREFIX)) {
                payload[key.removePrefix(PAYLOAD_PREFIX)] = value
            } else {
                metadata[key] = value
            }
        }

        return DeserializedMessage(
            metadata = metadata,
            payload = payload
        )
    }

    /**
     * 역직렬화 결과
     */
    data class DeserializedMessage(
        val metadata: Map<String, String>,
        val payload: Map<String, String>
    )
}
