package com.hirelog.api.job.application.intake

import com.hirelog.api.job.application.messaging.JdPreprocessRequestMessage
import com.hirelog.api.job.domain.JobSourceType
import com.hirelog.api.job.infra.kafka.publisher.JdPreprocessTextRequestPublisher
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
class TextJdIntakeService(
    private val jdPreprocessTextRequestPublisher: JdPreprocessTextRequestPublisher
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

        // === 식별자 생성 ===
        val eventId = UUID.randomUUID().toString()
        val requestId = UUID.randomUUID().toString()

        // === Kafka Event 생성 ===
        val message = JdPreprocessRequestMessage(
            eventId = eventId,
            requestId = requestId,
            occurredAt = System.currentTimeMillis(),
            version = "v1",
            brandName = brandName,
            positionName = positionName,
            source = source,
            text = rawText
        )

        // === Kafka 발행 ===
        jdPreprocessTextRequestPublisher.publish(message)

        return requestId
    }
}
