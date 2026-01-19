package com.hirelog.api.job.application.summary.command

import com.hirelog.api.brand.domain.Brand
import com.hirelog.api.common.config.properties.LlmProperties
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
            companyId = null,      // Brand-centric 모델: JobSummary 생성 시점에는 Company를 연결하지 않는다
            companyName = null,    // Company 매핑은 후속 파이프라인 또는 수동 검증 단계에서 수행
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
