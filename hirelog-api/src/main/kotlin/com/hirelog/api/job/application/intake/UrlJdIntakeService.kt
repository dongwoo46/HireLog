package com.hirelog.api.job.application.intake

import com.hirelog.api.common.infra.redis.messaging.RedisStreamPublisher
import com.hirelog.api.common.infra.redis.messaging.RedisStreamSerializer
import com.hirelog.api.common.logging.log
import com.hirelog.api.job.application.messaging.JdStreamKeys
import com.hirelog.api.job.domain.JobSourceType
import org.springframework.stereotype.Service
import java.util.UUID

/**
 * URL 기반 JD 수집 서비스
 *
 * 책임:
 * - URL 전처리 요청 메시지 발행
 *
 * 비책임:
 * - URL 크롤링/파싱 ❌ (Python에서 수행)
 * - 중복 판단 ❌ (Facade에서 수행)
 */
@Service
class UrlJdIntakeService(
    private val redisStreamPublisher: RedisStreamPublisher
) {

    /**
     * URL JD 요약 요청
     *
     * @param brandName 브랜드명
     * @param positionName 포지션명
     * @param url 채용공고 URL
     * @return requestId
     */
    fun requestUrlSummary(
        brandName: String,
        positionName: String,
        url: String
    ): String {
        require(brandName.isNotBlank()) { "brandName must not be blank" }
        require(positionName.isNotBlank()) { "positionName must not be blank" }
        require(url.isNotBlank()) { "url must not be blank" }
        require(isValidUrl(url)) { "Invalid URL format: $url" }

        val requestId = UUID.randomUUID().toString()

        log.info(
            "[URL_INTAKE] requestId={}, brandName={}, positionName={}, url={}",
            requestId, brandName, positionName, url
        )

        // Redis Stream 메시지 발행
        val message = RedisStreamSerializer.serialize(
            metadata = mapOf(
                "type" to JdMessageType.JD_PREPROCESS_REQUEST.name,
                "requestId" to requestId,
                "brandName" to brandName,
                "positionName" to positionName,
                "createdAt" to System.currentTimeMillis().toString(),
                "messageVersion" to "v1"
            ),
            payload = mapOf(
                "source" to JobSourceType.URL.name,
                "sourceUrl" to url
            )
        )

        redisStreamPublisher.publish(
            streamKey = JdStreamKeys.PREPROCESS_URL_REQUEST,
            message = message
        )

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
