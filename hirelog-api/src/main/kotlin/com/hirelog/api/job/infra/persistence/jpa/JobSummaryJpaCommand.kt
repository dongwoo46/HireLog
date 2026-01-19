package com.hirelog.api.job.infra.persistence.jpa


import com.hirelog.api.common.domain.LlmProvider
import com.hirelog.api.job.application.summary.command.JobSummaryCommand
import com.hirelog.api.job.domain.JobSummary
import com.hirelog.api.job.domain.CareerType
import com.hirelog.api.job.infrastructure.persistence.jpa.JobSummaryJpaRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * JobSummary JPA Command Adapter
 *
 * 책임:
 * - JobSummary 저장
 * - LLM 결과를 Entity로 영속화
 */
@Component
class JobSummaryJpaCommand(
    private val repository: JobSummaryJpaRepository
) : JobSummaryCommand {

    override fun create(
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
        llmProvider: LlmProvider,
        llmModel: String
    ): JobSummary {

        val summary = JobSummary.create(
            jobSnapshotId = jobSnapshotId,
            brandId = brandId,
            brandName = brandName,
            companyId = companyId,
            companyName = companyName,
            positionId = positionId,
            positionName = positionName,
            careerType = careerType,
            careerYears = careerYears,
            summaryText = summaryText,
            responsibilities = responsibilities,
            requiredQualifications = requiredQualifications,
            preferredQualifications = preferredQualifications,
            techStack = techStack,
            recruitmentProcess = recruitmentProcess,

            // ⭐ LLM 메타데이터 그대로 전달
            llmProvider = llmProvider,
            llmModel = llmModel
        )

        return repository.save(summary)
    }
}
