package com.hirelog.api.job.infra.external.gemini

import JobSummaryLlmRawResult
import com.hirelog.api.common.domain.LlmProvider
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
     * - summary / responsibilities / requiredQualifications 는 필수
     * - careerType / careerYears 는 파서에 위임
     */
    fun assemble(
        raw: JobSummaryLlmRawResult,
        provider: LlmProvider
    ): JobSummaryLlmResult {

        return JobSummaryLlmResult(
            llmProvider = provider,

            careerType = CareerType.from(raw.careerType),
            careerYears = CareerYearParser.parse(raw.careerYears),

            summary = raw.summary
                ?: throw IllegalStateException("summary missing"),

            responsibilities = raw.responsibilities
                ?: throw IllegalStateException("responsibilities missing"),

            requiredQualifications = raw.requiredQualifications
                ?: throw IllegalStateException("requiredQualifications missing"),

            preferredQualifications = raw.preferredQualifications,
            techStack = raw.techStack,
            recruitmentProcess = raw.recruitmentProcess,
            brandName = raw.brandName ?: throw IllegalStateException("brandName missing"),
            positionName = raw.positionName ?: throw IllegalStateException("positionName missing")
        )
    }
}
