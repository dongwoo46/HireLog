package com.hirelog.api.job.application.summary

import com.fasterxml.jackson.databind.ObjectMapper
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
import com.hirelog.api.job.domain.model.JobSummary
import com.hirelog.api.job.domain.model.JobSummaryInsight
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * JobSummary Write Application Service
 *
 * 책임:
 * - JobSummary 생성 유스케이스 수행
 * - Domain 객체 조합 및 정책 적용
 * - 저장은 Command Port에 위임
 */
@Service
class JobSummaryWriteService(
    private val objectMapper: ObjectMapper,
    private val summaryCommand: JobSummaryCommand,
    private val outboxEventCommand: OutboxEventCommand,
    private val processingCommand: JdSummaryProcessingCommand,
    private val processingQuery: JdSummaryProcessingQuery,
    private val llmProperties: LlmProperties
) {


    /**
     * JobSummary 생성 및 저장
     *
     * 트랜잭션:
     * - DB 변경만 포함
     * - LLM 호출 ❌ (Facade에서 이미 수행됨)
     */
    @Transactional
    fun save(
        snapshotId: Long,
        brand: Brand,
        positionId: Long,
        positionName: String,
        brandPositionId: Long,
        brandPositionName: String,
        positionCategoryId: Long,
        positionCategoryName: String,
        llmResult: JobSummaryLlmResult,
        sourceUrl: String? = null
    ): JobSummary {

        log.info(
            "[JOB_SUMMARY_CREATE] snapshotId={}, brandId={}, brandName='{}', positionId={}, positionName='{}', careerType={}, careerYears={}",
            snapshotId,
            brand.id, brand.name,
            positionId, positionName,
            llmResult.careerType, llmResult.careerYears
        )

        // Insight VO 생성
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

        return summaryCommand.save(summary)
    }

    /**
     * JobSummary 비활성화 + OpenSearch 삭제 이벤트 발행
     *
     * 용도:
     * - 잘못된 데이터 숨김
     * - 중복 데이터 처리
     * - 삭제 대신 소프트 삭제
     *
     * 트랜잭션:
     * - 비활성화 + Outbox 이벤트 발행이 원자적으로 처리됨
     */
    @Transactional
    fun deactivate(summaryId: Long) {
        val summary = summaryCommand.findById(summaryId)
            ?: throw IllegalArgumentException("JobSummary not found. id=$summaryId")

        summary.deactivate()
        summaryCommand.update(summary)

        // OpenSearch 삭제 이벤트 발행
        val outboxEvent = OutboxEvent.occurred(
            aggregateType = AggregateType.JOB_SUMMARY,
            aggregateId = summary.id.toString(),
            eventType = EventType.DELETED,
            payload = """{"id":${summary.id}}"""
        )
        outboxEventCommand.save(outboxEvent)

        log.info("[JOB_SUMMARY_DEACTIVATED] summaryId={}", summaryId)
    }

    /**
     * JobSummary 재활성화 + OpenSearch 인덱싱 이벤트 발행
     *
     * 용도:
     * - 잘못 비활성화된 데이터 복구
     *
     * 트랜잭션:
     * - 활성화 + Outbox 이벤트 발행이 원자적으로 처리됨
     */
    @Transactional
    fun activate(summaryId: Long) {
        val summary = summaryCommand.findById(summaryId)
            ?: throw IllegalArgumentException("JobSummary not found. id=$summaryId")

        summary.activate()
        summaryCommand.update(summary)

        // OpenSearch 재인덱싱 이벤트 발행
        val payload = JobSummaryOutboxPayload.from(summary)
        val outboxEvent = OutboxEvent.occurred(
            aggregateType = AggregateType.JOB_SUMMARY,
            aggregateId = summary.id.toString(),
            eventType = EventType.CREATED,
            payload = objectMapper.writeValueAsString(payload)
        )
        outboxEventCommand.save(outboxEvent)

        log.info("[JOB_SUMMARY_ACTIVATED] summaryId={}", summaryId)
    }
}
