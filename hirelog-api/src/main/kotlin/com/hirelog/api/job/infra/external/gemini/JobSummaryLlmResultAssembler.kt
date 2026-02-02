package com.hirelog.api.job.infra.external.gemini

import com.hirelog.api.common.domain.LlmProvider
import com.hirelog.api.job.application.summary.view.JobSummaryInsightResult
import com.hirelog.api.job.application.summary.view.JobSummaryLlmRawResult
import com.hirelog.api.job.application.summary.view.JobSummaryLlmResult
import com.hirelog.api.job.domain.CareerType

/**
 * JobSummaryLlmResultAssembler
 *
 * 책임:
 * - Raw LLM 응답을 시스템이 신뢰 가능한 Result로 변환
 * - enum / 필수 필드 보정
 * - LLM Provider 주입
 */
class JobSummaryLlmResultAssembler {

    /**
     * Raw → Result 변환
     *
     * 규칙:
     * - brandName / positionName / summary / responsibilities / requiredQualifications 는 필수
     * - careerType 파싱 실패 시 UNKNOWN
     * - Insight 필드들은 모두 nullable
     */
    fun assemble(
        raw: JobSummaryLlmRawResult,
        provider: LlmProvider
    ): JobSummaryLlmResult {

        return JobSummaryLlmResult(
            llmProvider = provider,

            // 기본 정보
            brandName = raw.brandName
                ?: throw IllegalStateException("brandName missing"),
            positionName = raw.positionName
                ?: throw IllegalStateException("positionName missing"),
            brandPositionName = raw.brandPositionName,
            companyCandidate = raw.companyCandidate,

            careerType = CareerType.from(raw.careerType),
            careerYears = raw.careerYears,

            // JD 요약
            summary = raw.summary
                ?: throw IllegalStateException("summary missing"),
            responsibilities = raw.responsibilities
                ?: throw IllegalStateException("responsibilities missing"),
            requiredQualifications = raw.requiredQualifications
                ?: throw IllegalStateException("requiredQualifications missing"),
            preferredQualifications = raw.preferredQualifications,
            techStack = raw.techStack,
            recruitmentProcess = raw.recruitmentProcess,

            // Insight
            insight = JobSummaryInsightResult(
                idealCandidate = raw.idealCandidate,
                mustHaveSignals = raw.mustHaveSignals,
                preparationFocus = raw.preparationFocus,
                transferableStrengthsAndGapPlan = raw.transferableStrengthsAndGapPlan,
                proofPointsAndMetrics = raw.proofPointsAndMetrics,
                storyAngles = raw.storyAngles,
                keyChallenges = raw.keyChallenges,
                technicalContext = raw.technicalContext,
                questionsToAsk = raw.questionsToAsk,
                considerations = raw.considerations
            )
        )
    }
}
