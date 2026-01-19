package com.hirelog.api.job.application.summary.command

import com.hirelog.api.brand.domain.Brand
import com.hirelog.api.config.properties.LlmProperties
import com.hirelog.api.job.application.summary.command.JobSummaryCommand
import com.hirelog.api.job.application.summary.port.JobSummaryLlmResult
import com.hirelog.api.job.domain.JobSnapshot
import com.hirelog.api.position.domain.Position
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class JobSummaryWriteService(
    private val summaryCommand: JobSummaryCommand,
    private val llmProperties: LlmProperties
) {

    /**
     * JobSummary 저장
     *
     * 트랜잭션 ⭕
     * - DB 변경만 담당
     */
    @Transactional
    fun save(
        snapshot: JobSnapshot,
        brand: Brand,
        position: Position,
        llmResult: JobSummaryLlmResult
    ) {
        summaryCommand.create(
            jobSnapshotId = snapshot.id,
            brandId = brand.id,
            brandName = brand.name,
            companyId = null,
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
    }
}
