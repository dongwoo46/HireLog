package com.hirelog.api.job.application.intake

import com.hirelog.api.common.application.outbox.OutboxEventWriteService
import com.hirelog.api.common.domain.outbox.AggregateType
import com.hirelog.api.common.domain.outbox.OutboxEvent
import com.hirelog.api.common.infra.storage.FileStorageService
import com.hirelog.api.job.application.messaging.JdPreprocessRequestMessage
import com.hirelog.api.job.application.summary.JobSummaryRequestWriteService
import com.hirelog.api.job.application.summary.port.JobSummaryQuery
import com.hirelog.api.job.domain.type.JobSourceType
import com.hirelog.api.common.logging.log
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.util.*

/**
 * JdIntakeService
 *
 * 역할:
 * - JD 유입 요청 처리
 * - JobSummaryRequest + Outbox 원자적 저장
 *
 * 트랜잭션 정책:
 * - JobSummaryRequest 생성과 Outbox 이벤트 저장을 하나의 트랜잭션에서 처리
 * - Debezium CDC가 Outbox를 감지하여 Kafka 전송 (dual-write 방지)
 */
@Service
class JdIntakeService(
    private val fileStorageService: FileStorageService,
    private val jobSummaryRequestWriteService: JobSummaryRequestWriteService,
    private val outboxEventWriteService: OutboxEventWriteService,
    private val jobSummaryQuery: JobSummaryQuery,
    private val objectMapper: ObjectMapper
) {

    /**
     * TEXT 기반 JD 전처리 요청
     */
    @Transactional
    fun requestText(
        memberId: Long,
        brandName: String,
        brandPositionName: String,
        text: String,
    ): String {
        require(brandName.isNotBlank()) { "brandName is required" }
        require(brandPositionName.isNotBlank()) { "positionName is required" }
        require(text.isNotBlank()) { "text is required" }

        val message = JdPreprocessRequestMessage(
            eventId = UUID.randomUUID().toString(),
            requestId = UUID.randomUUID().toString(),
            occurredAt = System.currentTimeMillis(),
            version = "v1",
            brandName = brandName,
            positionName = brandPositionName,
            source = JobSourceType.TEXT,
            text = text,
        )

        jobSummaryRequestWriteService.createRequest(memberId, message.requestId)
        appendOutbox(AggregateType.JD_PREPROCESS_TEXT, message)

        log.info(
            "[JD_INTAKE_TEXT_REQUESTED] memberId={}, requestId={}, brandName={}, positionName={}",
            memberId, message.requestId, brandName, brandPositionName
        )

        return message.requestId
    }

    /**
     * OCR 기반 JD 전처리 요청
     */
    @Transactional
    fun requestOcr(
        memberId: Long,
        brandName: String,
        brandPositionName: String,
        imageFiles: List<MultipartFile>,
    ): String {
        require(brandName.isNotBlank()) { "brandName is required" }
        require(brandPositionName.isNotBlank()) { "positionName is required" }
        require(imageFiles.isNotEmpty()) { "imageFiles is required" }

        val savedPaths = fileStorageService.saveImages(imageFiles, "ocr")

        val message = JdPreprocessRequestMessage(
            eventId = UUID.randomUUID().toString(),
            requestId = UUID.randomUUID().toString(),
            occurredAt = System.currentTimeMillis(),
            version = "v1",
            brandName = brandName,
            positionName = brandPositionName,
            source = JobSourceType.IMAGE,
            images = savedPaths,
        )

        jobSummaryRequestWriteService.createRequest(memberId, message.requestId)
        appendOutbox(AggregateType.JD_PREPROCESS_OCR, message)

        log.info(
            "[JD_INTAKE_OCR_REQUESTED] memberId={}, requestId={}, brandName={}, positionName={}, imageCount={}",
            memberId, message.requestId, brandName, brandPositionName, imageFiles.size
        )

        return message.requestId
    }

    /**
     * URL 기반 JD 전처리 요청
     *
     * 정책:
     * - 동일 URL로 생성된 JobSummary가 이미 존재하면 Duplicate 반환
     * - 신규 URL이면 파이프라인 진입 후 NewRequest 반환
     */
    @Transactional
    fun requestUrl(
        memberId: Long,
        brandName: String,
        brandPositionName: String,
        url: String,
    ): UrlIntakeResult {
        require(brandName.isNotBlank()) { "brandName is required" }
        require(brandPositionName.isNotBlank()) { "positionName is required" }
        require(isValidUrl(url)) { "Invalid URL format: $url" }

        val existing = jobSummaryQuery.findBySourceUrl(url)
        if (existing != null) {
            log.info("[JD_INTAKE_URL_DUPLICATE] url={}, existingSummaryId={}", url, existing.summaryId)
            return UrlIntakeResult.Duplicate(existing)
        }

        val message = JdPreprocessRequestMessage(
            eventId = UUID.randomUUID().toString(),
            requestId = UUID.randomUUID().toString(),
            occurredAt = System.currentTimeMillis(),
            version = "v1",
            brandName = brandName,
            positionName = brandPositionName,
            source = JobSourceType.URL,
            url = url,
        )

        jobSummaryRequestWriteService.createRequest(memberId, message.requestId)
        appendOutbox(AggregateType.JD_PREPROCESS_URL, message)

        log.info(
            "[JD_INTAKE_URL_REQUESTED] memberId={}, requestId={}, brandName={}, positionName={}, url={}",
            memberId, message.requestId, brandName, brandPositionName, url
        )

        return UrlIntakeResult.NewRequest(message.requestId)
    }

    private fun appendOutbox(
        aggregateType: AggregateType,
        message: JdPreprocessRequestMessage
    ) {
        try {
            val outboxEvent = OutboxEvent.occurred(
                aggregateType = aggregateType,
                aggregateId = message.requestId,
                eventType = "JD_PREPROCESS_REQUESTED",
                payload = objectMapper.writeValueAsString(message)
            )
            outboxEventWriteService.append(outboxEvent)
            log.info(
                "[JD_INTAKE_OUTBOX_APPENDED] aggregateType={}, requestId={}",
                aggregateType, message.requestId
            )
        } catch (e: Exception) {
            log.error(
                "[JD_INTAKE_OUTBOX_FAILED] aggregateType={}, requestId={}, error={}",
                aggregateType, message.requestId, e.message, e
            )
            throw e
        }
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
