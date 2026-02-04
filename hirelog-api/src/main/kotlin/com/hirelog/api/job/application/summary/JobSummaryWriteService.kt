package com.hirelog.api.job.application.summary

import com.hirelog.api.brand.domain.Brand
import com.hirelog.api.common.config.properties.LlmProperties
import com.hirelog.api.common.logging.log
import com.hirelog.api.job.application.summary.port.JobSummaryCommand
import com.hirelog.api.job.application.summary.view.JobSummaryLlmResult
import com.hirelog.api.job.domain.JobSummary
import com.hirelog.api.job.domain.JobSummaryInsight
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
    private val summaryCommand: JobSummaryCommand,
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
        brandPositionId: Long?,
        positionCategoryId: Long,
        positionCategoryName: String,
        llmResult: JobSummaryLlmResult,
        brandPositionName: String?,
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

        val summary = JobSummary.create(
            jobSnapshotId = snapshotId,
            brandId = brand.id,
            brandName = brand.name,
            companyId = null,
            companyName = null,
            positionId = positionId,
            positionName = positionName,
            brandPositionId = brandPositionId,
            brandPositionName = llmResult.brandPositionName,
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
}
