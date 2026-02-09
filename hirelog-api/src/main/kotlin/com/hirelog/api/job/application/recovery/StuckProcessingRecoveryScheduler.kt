package com.hirelog.api.job.application.recovery

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.hirelog.api.brand.application.BrandWriteService
import com.hirelog.api.brand.domain.BrandSource
import com.hirelog.api.relation.application.brandposition.BrandPositionWriteService
import com.hirelog.api.relation.domain.type.BrandPositionSource
import com.hirelog.api.common.logging.log
import com.hirelog.api.common.utils.Normalizer
import com.hirelog.api.job.application.jdsummaryprocessing.port.JdSummaryProcessingCommand
import com.hirelog.api.job.application.jdsummaryprocessing.port.JdSummaryProcessingQuery
import com.hirelog.api.job.application.summary.JobSummaryCreationService
import com.hirelog.api.job.application.summary.view.JobSummaryLlmResult
import com.hirelog.api.job.domain.model.JdSummaryProcessing
import com.hirelog.api.job.domain.type.JdSummaryProcessingStatus
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
    private val summaryCreationService: JobSummaryCreationService,
    private val brandWriteService: BrandWriteService,
    private val brandPositionWriteService: BrandPositionWriteService,
    private val positionCommand: PositionCommand,
    private val objectMapper: ObjectMapper
) {

    companion object {
        private const val STUCK_THRESHOLD_MINUTES = 30L
        private const val BATCH_SIZE = 50
        private const val MAX_RETRY_COUNT = 3
        private const val UNKNOWN_POSITION_NAME = "UNKNOWN"
    }

    /**
     * 10분마다 Stuck Processing 복구 시도
     */
    @Scheduled(cron = "0 */30 * * * *")
    fun recoverStuckProcessing() {
        log.info("[STUCK_PROCESSING_RECOVERY_START]")

        val olderThan = LocalDateTime.now().minusMinutes(STUCK_THRESHOLD_MINUTES)

        val stuckList = processingQuery.findStuckWithLlmResult(
            status = JdSummaryProcessingStatus.SUMMARIZING,
            olderThan = olderThan,
            limit = BATCH_SIZE
        )

        if (stuckList.isEmpty()) {
            log.info("[STUCK_PROCESSING_RECOVERY_SKIP] No stuck processing found")
            return
        }

        log.info("[STUCK_PROCESSING_RECOVERY_FOUND] count={}", stuckList.size)

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

        // Brand 조회/생성
        val brand = brandWriteService.getOrCreate(
            name = llmResult.brandName,
            companyId = null,
            source = BrandSource.INFERRED
        )

        // Position 조회 (LLM이 후보군에서 선택한 positionName 사용)
        val normalizedPositionName = Normalizer.normalizePosition(llmResult.positionName)
        val position = positionCommand.findByNormalizedName(normalizedPositionName)
            ?: positionCommand.findByNormalizedName(Normalizer.normalizePosition(UNKNOWN_POSITION_NAME))
            ?: throw IllegalStateException("UNKNOWN position not found")

        // BrandPosition 조회/생성 (원본 command의 positionName = displayName)
        val brandPosition = brandPositionWriteService.getOrCreate(
            brandId = brand.id,
            positionId = position.id,
            displayName = commandPositionName,
            source = BrandPositionSource.LLM
        )

        // 단일 트랜잭션으로 Summary + Outbox + Processing 완료
        summaryCreationService.createWithOutbox(
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

        log.info(
            "[STUCK_PROCESSING_RECOVERY_SUCCESS] processingId={}",
            processing.id
        )
    }

    /**
     * 복구 실패 시 FAILED 상태로 전환
     *
     * 정책:
     * - 현재는 1회 실패 시 바로 FAILED 처리
     * - 추후 재시도 횟수 추적 필요 시 필드 추가
     */
    @Transactional
    fun markAsFailedIfExhausted(processing: JdSummaryProcessing, exception: Exception) {
        processing.markFailed(
            errorCode = "RECOVERY_FAILED",
            errorMessage = "Stuck recovery failed: ${exception.message}"
        )
        processingCommand.update(processing)

        log.warn(
            "[STUCK_PROCESSING_MARKED_FAILED] processingId={}",
            processing.id
        )
    }
}
