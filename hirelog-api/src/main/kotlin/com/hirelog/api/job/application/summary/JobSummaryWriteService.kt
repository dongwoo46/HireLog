package com.hirelog.api.job.application.summary

import com.hirelog.api.brand.domain.Brand
import com.hirelog.api.common.config.properties.LlmProperties
import com.hirelog.api.job.application.summary.port.JobSummaryCommand
import com.hirelog.api.job.application.summary.view.JobSummaryLlmResult
import com.hirelog.api.job.domain.JobSummary
import com.hirelog.api.job.domain.JobSnapshot
import com.hirelog.api.position.domain.Position
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
        snapshot: JobSnapshot,
        brand: Brand,
        position: Position,
        llmResult: JobSummaryLlmResult
    ): JobSummary {
        // 1️⃣ Domain 객체 생성
        val summary = JobSummary.create(
            jobSnapshotId = snapshot.id,
            brandId = brand.id,
            brandName = brand.name,
            companyId = null,       // Brand-centric 모델 유지
            companyName = null,
            positionId = position.id,
            positionName = position.name,

            careerType = llmResult.careerType,
            careerYears = llmResult.careerYears,

            summaryText = llmResult.summary,
            responsibilities = llmResult.responsibilities,
            requiredQualifications = llmResult.requiredQualifications,
            preferredQualifications = llmResult.preferredQualifications,
            techStack = llmResult.techStack,
            recruitmentProcess = llmResult.recruitmentProcess,

            llmProvider = llmProperties.provider,
            llmModel = llmProperties.model
        )

        // 2️⃣ 저장
        return summaryCommand.save(summary)
    }
}
