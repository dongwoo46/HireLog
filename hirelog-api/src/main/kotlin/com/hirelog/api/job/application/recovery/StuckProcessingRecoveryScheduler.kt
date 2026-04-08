package com.hirelog.api.job.application.recovery

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.hirelog.api.brand.application.BrandWriteService
import com.hirelog.api.brand.domain.BrandSource
import com.hirelog.api.relation.application.brandposition.BrandPositionWriteService
import com.hirelog.api.relation.domain.type.BrandPositionSource
import com.hirelog.api.common.application.sse.SseEmitterManager
import com.hirelog.api.common.logging.log
import org.slf4j.MDC
import com.hirelog.api.common.utils.Normalizer
import com.hirelog.api.job.application.jdsummaryprocessing.port.JdSummaryProcessingCommand
import com.hirelog.api.job.application.jdsummaryprocessing.port.JdSummaryProcessingQuery
import com.hirelog.api.job.application.summary.JobSummaryWriteService
import com.hirelog.api.job.application.summary.JobSummaryRequestWriteService
import com.hirelog.api.job.application.summary.SnapshotAlreadySummarizedException
import com.hirelog.api.job.application.summary.view.JobSummaryLlmResult
import com.hirelog.api.job.domain.model.JdSummaryProcessing
import com.hirelog.api.job.domain.type.JdSummaryProcessingStatus
import com.hirelog.api.notification.application.NotificationWriteService
import com.hirelog.api.notification.domain.type.NotificationType
import com.hirelog.api.position.application.port.PositionCommand
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * Stuck Processing 복구 스케줄러
 *
 * 책임:
 * - SUMMARIZING 상태에서 장시간 멈춘 Processing 복구
 * - LLM 결과가 임시 저장되어 있는 경우 Post-LLM 재처리
 *
 * 실행 조건:
 * - status = SUMMARIZING
 * - llmResultJson IS NOT NULL
 * - updatedAt이 STUCK_THRESHOLD_MINUTES 이상 경과
 */
@Component
class StuckProcessingRecoveryScheduler(
    private val processingQuery: JdSummaryProcessingQuery,
    private val processingCommand: JdSummaryProcessingCommand,
    private val summaryWriteService: JobSummaryWriteService,
    private val brandWriteService: BrandWriteService,
    private val brandPositionWriteService: BrandPositionWriteService,
    private val positionCommand: PositionCommand,
    private val objectMapper: ObjectMapper,
    private val jobSummaryRequestWriteService: JobSummaryRequestWriteService,
    private val notificationWriteService: NotificationWriteService,
    private val sseEmitterManager: SseEmitterManager
) {

    companion object {
        private const val STUCK_THRESHOLD_MINUTES = 30L
        private const val BATCH_SIZE = 50
        private const val MAX_RETRY_COUNT = 3
        private const val UNKNOWN_VALUE = "UNKNOWN"
    }

    /**
     * 10분마다 Stuck Processing 복구 시도
     *
     * 동작:
     * - SUMMARIZING, POST_LLM_FAILED 상태 중 오래 멈춘 건을 조회한다.
     * - 각 건에 대해 Post-LLM 재처리를 시도한다.
     * - 복구 실패 시 markAsFailedIfExhausted()를 호출해 상태/요청 정리를 수행한다.
     */
    @Scheduled(cron = "0 */30 * * * *")
    fun recoverStuckProcessing() {
        log.debug("[STUCK_PROCESSING_RECOVERY_START]")

        val olderThan = LocalDateTime.now().minusMinutes(STUCK_THRESHOLD_MINUTES)

        val stuckList = processingQuery.findStuckWithLlmResult(
            statuses = listOf(
                JdSummaryProcessingStatus.SUMMARIZING,
                JdSummaryProcessingStatus.POST_LLM_FAILED
            ),
            olderThan = olderThan,
            limit = BATCH_SIZE
        )

        if (stuckList.isEmpty()) {
            log.info("[STUCK_PROCESSING_RECOVERY_SKIP] No stuck processing found")
            return
        }

        log.debug("[STUCK_PROCESSING_RECOVERY_FOUND] count={}", stuckList.size)

        var successCount = 0
        var failCount = 0

        stuckList.forEach { processing ->
            try {
                recoverSingleProcessing(processing)
                successCount++
            } catch (e: Exception) {
                log.error(
                    "[STUCK_PROCESSING_RECOVERY_FAILED] processingId={}, error={}",
                    processing.id, e.message, e
                )
                markAsFailedIfExhausted(processing, e)
                failCount++
            }
        }

        log.info(
            "[STUCK_PROCESSING_RECOVERY_COMPLETE] success={}, fail={}",
            successCount, failCount
        )
    }

    private fun recoverSingleProcessing(processing: JdSummaryProcessing) {
        MDC.put("processingId", processing.id.toString())
        try {

        val llmResultJson = processing.llmResultJson
            ?: throw IllegalStateException("llmResultJson is null for processing ${processing.id}")

        val snapshotId = processing.jobSnapshotId
            ?: throw IllegalStateException("jobSnapshotId is null for processing ${processing.id}")

        val commandBrandName = processing.commandBrandName
            ?: throw IllegalStateException("commandBrandName is null for processing ${processing.id}")

        val commandPositionName = processing.commandPositionName
            ?: throw IllegalStateException("commandPositionName is null for processing ${processing.id}")

        log.info(
            "[STUCK_PROCESSING_RECOVERY_ATTEMPT] processingId={}, snapshotId={}",
            processing.id, snapshotId
        )

        val llmResult = objectMapper.readValue<JobSummaryLlmResult>(llmResultJson)
        require(!isUnknownValue(llmResult.brandName)) {
            "UNKNOWN brandName is not allowed in recovery"
        }
        require(!isUnknownValue(llmResult.positionName)) {
            "UNKNOWN positionName is not allowed in recovery"
        }

        // Brand 조회/생성
        val brand = brandWriteService.getOrCreate(
            name = llmResult.brandName,
            companyId = null,
            source = BrandSource.INFERRED
        )

        // Position 조회 (LLM이 후보군에서 선택한 positionName 사용)
        val normalizedPositionName = Normalizer.normalizePosition(llmResult.positionName)
        val position = positionCommand.findByNormalizedName(normalizedPositionName)
            ?: throw IllegalStateException("Position not found for '${llmResult.positionName}'")

        // BrandPosition 조회/생성 (원본 command의 positionName = displayName)
        val brandPosition = brandPositionWriteService.getOrCreate(
            brandId = brand.id,
            positionId = position.id,
            displayName = commandPositionName,
            source = BrandPositionSource.LLM
        )

        // 단일 트랜잭션으로 Summary + Outbox + Processing 완료
        try {
            summaryWriteService.createWithOutbox(
                processingId = processing.id,
                snapshotId = snapshotId,
                brand = brand,
                positionId = position.id,
                positionName = position.name,
                brandPositionId = brandPosition.id,
                positionCategoryId = position.category.id,
                positionCategoryName = position.category.name,
                llmResult = llmResult,
                brandPositionName = commandPositionName,
                sourceUrl = null
            )
        } catch (e: SnapshotAlreadySummarizedException) {
            log.warn(
                "[STUCK_PROCESSING_RECOVERY_DUPLICATE] processingId={}, snapshotId={}, reason={}",
                processing.id, snapshotId, e.message
            )
            summaryWriteService.completeWithExistingSummary(
                processingId = processing.id,
                snapshotId = snapshotId
            )
        }

        log.info(
            "[STUCK_PROCESSING_RECOVERY_SUCCESS] processingId={}",
            processing.id
        )

        } finally {
            MDC.clear()
        }
    }

    /**
     * 복구 실패 시 FAILED 상태로 전환
     *
     * 정책:
     * - markFailed()는 도메인 규칙상 RECEIVED/SUMMARIZING에서만 허용된다.
     * - POST_LLM_FAILED 등 종결 상태는 markFailed()로 덮어쓰지 않는다.
     * - 상태 전환 여부와 무관하게 JobSummaryRequest는 fail 처리하고 알림을 발송한다.
     */
    @Transactional
    fun markAsFailedIfExhausted(processing: JdSummaryProcessing, exception: Exception) {
        if (
            processing.status == JdSummaryProcessingStatus.RECEIVED ||
            processing.status == JdSummaryProcessingStatus.SUMMARIZING
        ) {
            processing.markFailed(
                errorCode = "RECOVERY_FAILED",
                errorMessage = "Stuck recovery failed: ${exception.message}"
            )
            processingCommand.update(processing)
        } else {
            log.warn(
                "[STUCK_PROCESSING_MARK_FAILED_SKIPPED] processingId={}, currentStatus={}",
                processing.id, processing.status
            )
        }

        // JobSummaryRequest 실패 처리 + SSE 알림
        val memberId = jobSummaryRequestWriteService.failRequest(
            requestId = processing.id.toString()
        )

        if (memberId != null) {
            try {
                notificationWriteService.create(
                    memberId = memberId,
                    type = NotificationType.JOB_SUMMARY_FAILED,
                    title = if (!processing.commandBrandName.isNullOrBlank() && !processing.commandPositionName.isNullOrBlank())
                        "${processing.commandBrandName} ${processing.commandPositionName} 분석 실패"
                    else
                        "채용공고 분석 실패",
                    message = "요청하신 채용공고 분석이 실패했습니다.",
                    metadata = mapOf(
                        "requestId" to processing.id.toString(),
                        "errorCode" to "RECOVERY_FAILED",
                        "retryable" to false
                    )
                )
            } catch (e: Exception) {
                log.error(
                    "[STUCK_PROCESSING_NOTIFICATION_CREATE_FAILED] processingId={}, memberId={}, error={}",
                    processing.id, memberId, e.message, e
                )
            }

            sseEmitterManager.send(
                memberId = memberId,
                eventName = "JOB_SUMMARY_FAILED",
                data = mapOf(
                    "requestId" to processing.id.toString(),
                    "errorCode" to "RECOVERY_FAILED",
                    "retryable" to false
                )
            )
        }

        log.error(
            "[STUCK_PROCESSING_MARKED_FAILED] processingId={}, brandName={}, positionName={}, errorMessage={}",
            processing.id, processing.commandBrandName, processing.commandPositionName, exception.message
        )
    }

    private fun isUnknownValue(value: String): Boolean =
        value.trim().equals(UNKNOWN_VALUE, ignoreCase = true)
}
