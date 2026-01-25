package com.hirelog.api.job.application.preprocess

import com.hirelog.api.common.infra.redis.messaging.RedisStreamPublisher
import com.hirelog.api.common.infra.redis.messaging.RedisStreamSerializer
import com.hirelog.api.job.application.messaging.JdStreamKeys
import com.hirelog.api.job.domain.JobSourceType
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

    fun requestSummary(
        brandName: String,
        positionName: String,
        rawText: String,
        source: JobSourceType
    ): String {

        require(brandName.isNotBlank()) { "brandName must not be blank" }
        require(positionName.isNotBlank()) { "positionHint must not be blank" }
        require(rawText.isNotBlank()) { "rawText must not be blank" }

        val requestId = UUID.randomUUID().toString()

        val message = RedisStreamSerializer.serialize(
            metadata = mapOf(
                "type" to JdMessageType.JD_PREPROCESS_REQUEST.name,
                "requestId" to requestId,
                "brandName" to brandName,
                "positionName" to positionName,

                // 메시지 메타
                "createdAt" to System.currentTimeMillis().toString(),
                "messageVersion" to "v1"
            ),
            payload = mapOf(
                "text" to rawText,
                "source" to source.name
            )
        )

        redisStreamPublisher.publish(
            streamKey = JdStreamKeys.PREPROCESS_TEXT_REQUEST,
            message = message
        )

        return requestId
    }
}
