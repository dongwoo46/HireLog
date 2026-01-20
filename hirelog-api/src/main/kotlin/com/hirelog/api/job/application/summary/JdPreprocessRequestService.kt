package com.hirelog.api.job.application.preprocess

import com.hirelog.api.common.infra.redis.messaging.RedisStreamPublisher
import com.hirelog.api.common.infra.redis.messaging.RedisStreamSerializer
import com.hirelog.api.jd.application.messaging.JdStreamKeys
import org.springframework.stereotype.Service
import java.util.UUID

/**
 * JD 전처리 요청 Application Service
 *
 * 책임:
 * - JD 전처리 비동기 파이프라인 진입
 *
 * 비책임:
 * - DB 저장 ❌
 * - 중복 판단 ❌
 * - 전처리 로직 ❌
 */
@Service
class JdPreprocessRequestService(
    private val redisStreamPublisher: RedisStreamPublisher
) {

    fun request(
        brandName: String,
        positionHint: String,
        rawText: String
    ): String {

        require(brandName.isNotBlank()) { "brandName must not be blank" }
        require(positionHint.isNotBlank()) { "positionHint must not be blank" }
        require(rawText.isNotBlank()) { "rawText must not be blank" }

        val requestId = UUID.randomUUID().toString()

        val message = RedisStreamSerializer.serialize(
            metadata = mapOf(
                "type" to JdMessageType.JD_PREPROCESS_REQUEST.name,
                "requestId" to requestId,
                "brandName" to brandName,
                "positionHint" to positionHint
            ),
            payload = mapOf(
                "text" to rawText
            )
        )

        redisStreamPublisher.publish(
            streamKey = JdStreamKeys.PREPROCESS,
            message = message
        )

        return requestId
    }
}
