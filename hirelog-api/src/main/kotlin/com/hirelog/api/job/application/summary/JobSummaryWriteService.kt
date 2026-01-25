package com.hirelog.api.job.application.summary

import com.hirelog.api.brand.domain.Brand
import com.hirelog.api.common.config.properties.LlmProperties
import com.hirelog.api.common.logging.log
import com.hirelog.api.job.application.summary.port.JobSummaryCommand
import com.hirelog.api.job.application.summary.view.JobSummaryLlmResult
import com.hirelog.api.job.domain.JobSummary
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
        snapshotId: Long,
        brand: Brand,
        position: Position,
        llmResult: JobSummaryLlmResult
    ): JobSummary {
        // ── [LOG] JobSummary 생성 직전 전체 필드 덤프 ─────────────────────
        log.info(
            """
        [JOB_SUMMARY_CREATE]
        snapshotId={}
        brandId={}, brandName='{}'
        positionId={}, positionName='{}'
        careerType={}, careerYears={}
        summaryTextLength={}
        responsibilitiesLength={}
        requiredQualificationsLength={}
        preferredQualificationsLength={}
        techStackLength={}
        recruitmentProcessLength={}
        llmProvider='{}'
        llmModel='{}'
        """.trimIndent(),
            snapshotId,
            brand.id, brand.name,
            position.id, position.name,
            llmResult.careerType, llmResult.careerYears,
            llmResult.summary,
            llmResult.responsibilities,
            llmResult.requiredQualifications,
            llmResult.preferredQualifications,
            llmResult.techStack,
            llmResult.recruitmentProcess,
            llmProperties.provider,
            llmProperties.model
        )
        val summary = JobSummary.create(
            jobSnapshotId = snapshotId,
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

        return summaryCommand.save(summary)
    }
}
