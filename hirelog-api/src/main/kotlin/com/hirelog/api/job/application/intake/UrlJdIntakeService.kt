package com.hirelog.api.job.application.intake

import com.hirelog.api.common.logging.log
import com.hirelog.api.job.application.messaging.JdPreprocessRequestMessage
import com.hirelog.api.job.domain.JobSourceType
import com.hirelog.api.job.infra.kafka.publisher.JdPreprocessUrlRequestPublisher
import org.springframework.stereotype.Service
import java.util.UUID

/**
 * URL 기반 JD 수집 서비스
 *
 * 책임:
 * - URL 전처리 요청 Kafka 이벤트 발행
 *
 * 비책임:
 * - URL 크롤링/파싱 ❌ (Python Worker)
 * - 중복 판단 ❌
 * - 요약 처리 ❌
 */
@Service
class UrlJdIntakeService(
    private val jdPreprocessUrlRequestPublisher: JdPreprocessUrlRequestPublisher
) {

    /**
     * URL JD 요약 요청
     *
     * @param brandName 브랜드명
     * @param positionName 포지션명
     * @param url 채용공고 URL
     * @return requestId (Correlation ID)
     */
    fun requestUrlSummary(
        brandName: String,
        positionName: String,
        url: String
    ): String {

        // === 입력 검증 ===
        require(brandName.isNotBlank()) { "brandName must not be blank" }
        require(positionName.isNotBlank()) { "positionName must not be blank" }
        require(url.isNotBlank()) { "url must not be blank" }
        require(isValidUrl(url)) { "Invalid URL format: $url" }

        // === 식별자 생성 ===
        val eventId = UUID.randomUUID().toString()
        val requestId = UUID.randomUUID().toString()

        log.info(
            "[URL_INTAKE] requestId={}, brandName={}, positionName={}, url={}",
            requestId,
            brandName,
            positionName,
            url
        )

        // === Kafka Event 생성 ===
        val message = JdPreprocessRequestMessage(
            eventId = eventId,
            requestId = requestId,
            occurredAt = System.currentTimeMillis(),
            version = "v1",
            brandName = brandName,
            positionName = positionName,
            source = JobSourceType.URL,
            text = url
        )

        // === Kafka 발행 (URL Request Topic) ===
        jdPreprocessUrlRequestPublisher.publish(message)

        return requestId
    }

    private fun isValidUrl(url: String): Boolean {
        return try {
            val uri = java.net.URI(url)
            uri.scheme in listOf("http", "https") && uri.host != null
        } catch (e: Exception) {
            false
        }
    }
}
