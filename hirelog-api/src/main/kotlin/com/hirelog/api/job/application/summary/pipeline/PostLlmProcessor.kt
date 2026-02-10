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
        val resolvedBrandPositionName: String
    )

    fun resolve(
        llmResult: JobSummaryLlmResult,
        fallbackPositionName: String
    ): ResolvedEntities {
        val brand = brandWriteService.getOrCreate(
            name = llmResult.brandName,
            companyId = null,
            source = BrandSource.INFERRED
        )

        val normalizedPositionName = Normalizer.normalizePosition(llmResult.positionName)

        val position: Position =
            positionCommand.findByNormalizedName(normalizedPositionName)
                ?: positionCommand.findByNormalizedName(
                    Normalizer.normalizePosition(UNKNOWN_POSITION_NAME)
                )
                ?: throw IllegalStateException("UNKNOWN position not found")

        val resolvedBrandPositionName =
            llmResult.brandPositionName?.takeIf { it.isNotBlank() }
                ?: fallbackPositionName

        val brandPosition = brandPositionWriteService.getOrCreate(
            brandId = brand.id,
            positionId = position.id,
            displayName = resolvedBrandPositionName,
            source = BrandPositionSource.LLM
        )

        return ResolvedEntities(
            brand = brand,
            position = position,
            brandPosition = brandPosition,
            resolvedBrandPositionName = resolvedBrandPositionName
        )
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
            brandPositionName = resolved.resolvedBrandPositionName,
            sourceUrl = command.sourceUrl
        )

        tryCreateCompanyCandidate(summary.id, resolved.brand.id, llmResult.companyCandidate)
    }

    /**
     * Admin용: createAllForAdmin + CompanyCandidate
     */
    fun executeForAdmin(
        snapshotCommand: JobSummaryWriteService.JobSnapshotCommand,
        llmResult: JobSummaryLlmResult,
        fallbackPositionName: String,
        sourceUrl: String?
    ): JobSummary {
        val resolved = resolve(llmResult, fallbackPositionName)

        val summary = summaryWriteService.createAllForAdmin(
            snapshotCommand = snapshotCommand,
            llmResult = llmResult,
            brand = resolved.brand,
            positionId = resolved.position.id,
            positionName = resolved.position.name,
            brandPositionId = resolved.brandPosition.id,
            brandPositionName = resolved.resolvedBrandPositionName,
            positionCategoryId = resolved.position.category.id,
            positionCategoryName = resolved.position.category.name,
            sourceUrl = sourceUrl
        )

        tryCreateCompanyCandidate(summary.id, resolved.brand.id, llmResult.companyCandidate)

        return summary
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
