package com.hirelog.api.job.application.summary.pipeline

import com.hirelog.api.brand.application.BrandWriteService
import com.hirelog.api.brand.domain.Brand
import com.hirelog.api.brand.domain.BrandSource
import com.hirelog.api.common.logging.log
import com.hirelog.api.common.utils.Normalizer
import com.hirelog.api.company.application.CompanyCandidateWriteService
import com.hirelog.api.company.domain.CompanyCandidateSource
import com.hirelog.api.job.application.summary.JobSummaryWriteService
import com.hirelog.api.job.application.summary.command.JobSummaryGenerateCommand
import com.hirelog.api.job.application.summary.view.JobSummaryLlmResult
import com.hirelog.api.job.domain.model.JobSummary
import com.hirelog.api.position.application.port.PositionCommand
import com.hirelog.api.position.domain.Position
import com.hirelog.api.relation.application.brandposition.BrandPositionWriteService
import com.hirelog.api.relation.domain.model.BrandPosition
import com.hirelog.api.relation.domain.type.BrandPositionSource
import org.springframework.stereotype.Service
import java.util.*

@Service
class PostLlmProcessor(
    private val brandWriteService: BrandWriteService,
    private val brandPositionWriteService: BrandPositionWriteService,
    private val positionCommand: PositionCommand,
    private val companyCandidateWriteService: CompanyCandidateWriteService,
    private val summaryWriteService: JobSummaryWriteService
) {

    companion object {
        private const val UNKNOWN_POSITION_NAME = "UNKNOWN"
        private const val LLM_COMPANY_CONFIDENCE_SCORE = 0.7
    }

    /**
     * LLM 결과에서 Brand, Position, BrandPosition 해석
     */
    data class ResolvedEntities(
        val brand: Brand,
        val position: Position,
        val brandPosition: BrandPosition,
    )

    fun resolve(
        llmResult: JobSummaryLlmResult,
        brandPositionName: String
    ): ResolvedEntities {
        try {
            val brand = brandWriteService.getOrCreate(
                name = llmResult.brandName,
                companyId = null,
                source = BrandSource.INFERRED
            )

            val normalizedPositionName =
                Normalizer.normalizePosition(llmResult.positionName)

            val position: Position =
                positionCommand.findByNormalizedName(normalizedPositionName)
                    ?: throw IllegalStateException("UNKNOWN position not found")


            val brandPosition = brandPositionWriteService.getOrCreate(
                brandId = brand.id,
                positionId = position.id,
                displayName = brandPositionName,
                source = BrandPositionSource.LLM
            )

            return ResolvedEntities(
                brand = brand,
                position = position,
                brandPosition = brandPosition
            )
        } catch (e: Exception) {
            log.error(
                "[RESOLVE_ENTITIES_FAILED] brandName='{}', positionName='{}', fallback='{}', error={}",
                llmResult.brandName, llmResult.positionName, brandPositionName, e.message, e
            )
            throw e
        }
    }

    /**
     * 파이프라인용: createWithOutbox + CompanyCandidate
     */
    fun execute(
        snapshotId: Long,
        llmResult: JobSummaryLlmResult,
        processingId: UUID,
        command: JobSummaryGenerateCommand
    ) {
        try {
            val resolved = resolve(llmResult, command.positionName)

            val summary = summaryWriteService.createWithOutbox(
                processingId = processingId,
                snapshotId = snapshotId,
                brand = resolved.brand,
                positionId = resolved.position.id,
                positionName = resolved.position.name,
                brandPositionId = resolved.brandPosition.id,
                positionCategoryId = resolved.position.category.id,
                positionCategoryName = resolved.position.category.name,
                llmResult = llmResult,
                brandPositionName = resolved.brandPosition.displayName,
                sourceUrl = command.sourceUrl
            )

            tryCreateCompanyCandidate(summary.id, resolved.brand.id, llmResult.companyCandidate)
        } catch (e: Exception) {
            log.error(
                "[EXECUTE_FAILED] snapshotId={}, processingId={}, brandName='{}', error={}",
                snapshotId, processingId, llmResult.brandName, e.message, e
            )
            throw e
        }
    }

    /**
     * Admin용: createAllForAdmin + CompanyCandidate
     */
    fun executeForAdmin(
        snapshotCommand: JobSummaryWriteService.JobSnapshotCommand,
        llmResult: JobSummaryLlmResult,
        brandPositionName: String,
        sourceUrl: String?
    ): JobSummary {
        try {
            val resolved = resolve(llmResult, brandPositionName)

            val summary = summaryWriteService.createAllForAdmin(
                snapshotCommand = snapshotCommand,
                llmResult = llmResult,
                brand = resolved.brand,
                positionId = resolved.position.id,
                positionName = resolved.position.name,
                brandPositionId = resolved.brandPosition.id,
                brandPositionName = resolved.brandPosition.displayName,
                positionCategoryId = resolved.position.category.id,
                positionCategoryName = resolved.position.category.name,
                sourceUrl = sourceUrl
            )

            tryCreateCompanyCandidate(summary.id, resolved.brand.id, llmResult.companyCandidate)

            return summary
        } catch (e: Exception) {
            log.error(
                "[EXECUTE_FOR_ADMIN_FAILED] brandName='{}', brandPositionName='{}', error={}",
                llmResult.brandName, brandPositionName, e.message, e
            )
            throw e
        }
    }

    private fun tryCreateCompanyCandidate(
        jdSummaryId: Long,
        brandId: Long,
        companyCandidate: String?
    ) {
        if (companyCandidate.isNullOrBlank()) return

        try {
            companyCandidateWriteService.createCandidate(
                jdSummaryId = jdSummaryId,
                brandId = brandId,
                candidateName = companyCandidate,
                source = CompanyCandidateSource.LLM,
                confidenceScore = LLM_COMPANY_CONFIDENCE_SCORE
            )
            log.info(
                "[COMPANY_CANDIDATE_CREATED] jdSummaryId={}, brandId={}, companyCandidate={}",
                jdSummaryId, brandId, companyCandidate
            )
        } catch (e: Exception) {
            log.warn(
                "[COMPANY_CANDIDATE_FAILED] jdSummaryId={}, brandId={}, companyCandidate={}, error={}",
                jdSummaryId, brandId, companyCandidate, e.message
            )
        }
    }
}
