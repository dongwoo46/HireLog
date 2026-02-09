package com.hirelog.api.job.application.summary

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.hirelog.api.brand.domain.Brand
import com.hirelog.api.common.application.outbox.OutboxEventCommand
import com.hirelog.api.common.config.properties.LlmProperties
import com.hirelog.api.common.domain.outbox.AggregateType
import com.hirelog.api.common.domain.outbox.OutboxEvent
import com.hirelog.api.common.logging.log
import com.hirelog.api.job.application.jdsummaryprocessing.port.JdSummaryProcessingCommand
import com.hirelog.api.job.application.jdsummaryprocessing.port.JdSummaryProcessingQuery
import com.hirelog.api.job.application.summary.JobSummaryOutboxConstants.EventType
import com.hirelog.api.job.application.summary.payload.JobSummaryOutboxPayload
import com.hirelog.api.job.application.summary.port.JobSummaryCommand
import com.hirelog.api.job.application.summary.view.JobSummaryLlmResult
import com.hirelog.api.job.domain.model.JobSnapshot
import com.hirelog.api.job.domain.model.JobSummary
import com.hirelog.api.job.domain.model.JobSummaryInsight
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * JobSummary 생성 통합 서비스
 *
 * 책임:
 * - JobSummary + Outbox + Processing 상태를 단일 트랜잭션으로 처리
 * - 원자성 보장: 하나라도 실패 시 전체 롤백
 *
 * 설계 원칙:
 * - Transactional Outbox 패턴의 핵심 구현
 * - 도메인 상태 변경과 이벤트 발행의 정합성 보장
 */
@Service
class JobSummaryCreationService(
    private val summaryCommand: JobSummaryCommand,
    private val outboxEventCommand: OutboxEventCommand,
    private val processingCommand: JdSummaryProcessingCommand,
    private val processingQuery: JdSummaryProcessingQuery,
    private val llmProperties: LlmProperties,
    private val jobSummaryRequestWriteService: JobSummaryRequestWriteService
) {

    private val objectMapper = ObjectMapper().apply {
        registerKotlinModule()
        registerModule(JavaTimeModule())
        disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    /**
     * JobSummary 생성 + Outbox 발행 + Processing 완료 (단일 트랜잭션)
     *
     * 트랜잭션 정책:
     * - 3개 작업이 모두 성공해야 커밋
     * - 하나라도 실패 시 전체 롤백
     * - LLM 결과는 이미 Processing에 임시 저장되어 있으므로 복구 가능
     */
    @Transactional
    fun createWithOutbox(
        processingId: UUID,
        snapshotId: Long,
        brand: Brand,
        positionId: Long,
        positionName: String,
        brandPositionId: Long,
        brandPositionName: String,
        positionCategoryId: Long,
        positionCategoryName: String,
        llmResult: JobSummaryLlmResult,
        sourceUrl: String?
    ): JobSummary {

        log.info(
            "[JOB_SUMMARY_CREATE_WITH_OUTBOX] processingId={}, snapshotId={}, brandId={}, brandName='{}'",
            processingId, snapshotId, brand.id, brand.name
        )

        // 1. JobSummary 생성 및 저장
        val insight = JobSummaryInsight.create(
            idealCandidate = llmResult.insight.idealCandidate,
            mustHaveSignals = llmResult.insight.mustHaveSignals,
            preparationFocus = llmResult.insight.preparationFocus,
            transferableStrengthsAndGapPlan = llmResult.insight.transferableStrengthsAndGapPlan,
            proofPointsAndMetrics = llmResult.insight.proofPointsAndMetrics,
            storyAngles = llmResult.insight.storyAngles,
            keyChallenges = llmResult.insight.keyChallenges,
            technicalContext = llmResult.insight.technicalContext,
            questionsToAsk = llmResult.insight.questionsToAsk,
            considerations = llmResult.insight.considerations
        )

        val resolvedBrandPositionName =
            llmResult.brandPositionName?.takeIf { it.isNotBlank() }
                ?: brandPositionName

        val summary = JobSummary.create(
            jobSnapshotId = snapshotId,
            brandId = brand.id,
            brandName = brand.name,
            companyId = null,
            companyName = null,
            positionId = positionId,
            positionName = positionName,
            brandPositionId = brandPositionId,
            brandPositionName = resolvedBrandPositionName,
            positionCategoryId = positionCategoryId,
            positionCategoryName = positionCategoryName,
            careerType = llmResult.careerType,
            careerYears = llmResult.careerYears,
            summaryText = llmResult.summary,
            responsibilities = llmResult.responsibilities,
            requiredQualifications = llmResult.requiredQualifications,
            preferredQualifications = llmResult.preferredQualifications,
            techStack = llmResult.techStack,
            recruitmentProcess = llmResult.recruitmentProcess,
            insight = insight,
            llmProvider = llmProperties.provider,
            llmModel = llmProperties.model,
            sourceUrl = sourceUrl
        )

        val savedSummary = summaryCommand.save(summary)

        // 2. Outbox 이벤트 저장 (동일 트랜잭션)
        val outboxEvent = OutboxEvent.occurred(
            aggregateType = AggregateType.JOB_SUMMARY,
            aggregateId = savedSummary.id.toString(),
            eventType = EventType.CREATED,
            payload = buildEventPayload(savedSummary)
        )
        outboxEventCommand.save(outboxEvent)

        // 3. Processing 완료 상태로 전이 (동일 트랜잭션)
        val processing = processingQuery.findById(processingId)
            ?: throw IllegalStateException("Processing not found. id=$processingId")

        processing.markCompleted(savedSummary.id)
        processingCommand.update(processing)

        // 4. JobSummaryRequest 완료 + MemberJobSummary 자동 생성 (동일 트랜잭션)
        jobSummaryRequestWriteService.completeRequests(
            requestId = processingId.toString(),
            summary = savedSummary
        )

        log.info(
            "[JOB_SUMMARY_CREATE_WITH_OUTBOX_SUCCESS] summaryId={}, processingId={}",
            savedSummary.id, processingId
        )

        return savedSummary
    }

    /**
     * Admin 전용: Snapshot + JobSummary + Outbox 전체 생성 (단일 트랜잭션)
     *
     * 용도:
     * - Admin에서 직접 생성 시 사용
     * - LLM 호출 완료 후 모든 데이터를 원자적으로 저장
     *
     * 트랜잭션 정책:
     * - Snapshot, JobSummary, Outbox 모두 같은 트랜잭션
     * - 하나라도 실패 시 전체 롤백 → Orphan 데이터 없음
     */
    @Transactional
    fun createAllForAdmin(
        snapshotCommand: JobSnapshotCommand,
        llmResult: JobSummaryLlmResult,
        brand: Brand,
        positionId: Long,
        positionName: String,
        brandPositionId: Long,
        brandPositionName: String,
        positionCategoryId: Long,
        positionCategoryName: String,
        sourceUrl: String?
    ): JobSummary {

        log.info(
            "[ADMIN_JOB_SUMMARY_CREATE_ALL] brandId={}, brandName='{}'",
            brand.id, brand.name
        )

        // 1. Snapshot 저장
        val snapshot = snapshotCommand.save()

        // 2. JobSummary 생성 및 저장
        val insight = JobSummaryInsight.create(
            idealCandidate = llmResult.insight.idealCandidate,
            mustHaveSignals = llmResult.insight.mustHaveSignals,
            preparationFocus = llmResult.insight.preparationFocus,
            transferableStrengthsAndGapPlan = llmResult.insight.transferableStrengthsAndGapPlan,
            proofPointsAndMetrics = llmResult.insight.proofPointsAndMetrics,
            storyAngles = llmResult.insight.storyAngles,
            keyChallenges = llmResult.insight.keyChallenges,
            technicalContext = llmResult.insight.technicalContext,
            questionsToAsk = llmResult.insight.questionsToAsk,
            considerations = llmResult.insight.considerations
        )

        val resolvedBrandPositionName =
            llmResult.brandPositionName?.takeIf { it.isNotBlank() }
                ?: brandPositionName

        val summary = JobSummary.create(
            jobSnapshotId = snapshot.id,
            brandId = brand.id,
            brandName = brand.name,
            companyId = null,
            companyName = null,
            positionId = positionId,
            positionName = positionName,
            brandPositionId = brandPositionId,
            brandPositionName = resolvedBrandPositionName,
            positionCategoryId = positionCategoryId,
            positionCategoryName = positionCategoryName,
            careerType = llmResult.careerType,
            careerYears = llmResult.careerYears,
            summaryText = llmResult.summary,
            responsibilities = llmResult.responsibilities,
            requiredQualifications = llmResult.requiredQualifications,
            preferredQualifications = llmResult.preferredQualifications,
            techStack = llmResult.techStack,
            recruitmentProcess = llmResult.recruitmentProcess,
            insight = insight,
            llmProvider = llmProperties.provider,
            llmModel = llmProperties.model,
            sourceUrl = sourceUrl
        )

        val savedSummary = summaryCommand.save(summary)

        // 3. Outbox 이벤트 저장 (동일 트랜잭션)
        val outboxEvent = OutboxEvent.occurred(
            aggregateType = AggregateType.JOB_SUMMARY,
            aggregateId = savedSummary.id.toString(),
            eventType = EventType.CREATED,
            payload = buildEventPayload(savedSummary)
        )
        outboxEventCommand.save(outboxEvent)

        log.info(
            "[ADMIN_JOB_SUMMARY_CREATE_ALL_SUCCESS] snapshotId={}, summaryId={}",
            snapshot.id, savedSummary.id
        )

        return savedSummary
    }

    /**
     * Snapshot 저장 Command (지연 실행용)
     */
    fun interface JobSnapshotCommand {
        fun save(): JobSnapshot
    }

    private fun buildEventPayload(summary: JobSummary): String {
        val payload = JobSummaryOutboxPayload.from(summary)
        return objectMapper.writeValueAsString(payload)
    }
}
