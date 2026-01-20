package com.hirelog.api.job.application.summary.command

import com.hirelog.api.common.domain.LlmProvider
import com.hirelog.api.job.domain.JobSummary
import com.hirelog.api.job.domain.CareerType

/**
 * JobSummary Command Port
 *
 * 책임:
 * - JobSummary 저장 유스케이스 계약
 * - 저장소 및 색인 전략을 알지 않는다
 */

interface JobSummaryCommand {

    /**
     * JobSummary 생성
     *
     * - LLM 선택/판단 ❌
     * - 전달받은 메타데이터를 그대로 저장
     */
    fun create(
        jobSnapshotId: Long,
        brandId: Long,
        brandName: String,
        companyId: Long?,
        companyName: String?,
        positionId: Long,
        positionName: String,
        careerType: CareerType,
        careerYears: Int?,
        summaryText: String,
        responsibilities: String,
        requiredQualifications: String,
        preferredQualifications: String?,
        techStack: String?,
        recruitmentProcess: String?,

        // ⭐ LLM 메타데이터
        llmProvider: LlmProvider,
        llmModel: String
    ): JobSummary
}