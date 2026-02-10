package com.hirelog.api.job.application.summary.pipeline

import com.hirelog.api.brand.application.BrandWriteService
import com.hirelog.api.brand.domain.BrandSource
import com.hirelog.api.common.utils.Normalizer
import com.hirelog.api.company.application.CompanyCandidateWriteService
import com.hirelog.api.company.domain.CompanyCandidateSource
import com.hirelog.api.job.application.summary.JobSummaryCreationService
import com.hirelog.api.job.application.summary.command.JobSummaryGenerateCommand
import com.hirelog.api.job.application.summary.view.JobSummaryLlmResult
import com.hirelog.api.position.application.port.PositionCommand
import com.hirelog.api.position.domain.Position
import com.hirelog.api.relation.application.brandposition.BrandPositionWriteService
import com.hirelog.api.relation.domain.type.BrandPositionSource
import org.springframework.stereotype.Service
import java.util.*

@Service
class PostLlmProcessor(
    private val brandWriteService: BrandWriteService,
    private val brandPositionWriteService: BrandPositionWriteService,
    private val positionCommand: PositionCommand,
    private val companyCandidateWriteService: CompanyCandidateWriteService,
    private val summaryCreationService: JobSummaryCreationService
) {
    companion object {
        private const val LLM_TIMEOUT_SECONDS = 45L
        private const val UNKNOWN_POSITION_NAME = "UNKNOWN"
        private const val LLM_COMPANY_CONFIDENCE_SCORE = 0.7
    }

    fun execute(
        snapshotId: Long,
        llmResult: JobSummaryLlmResult,
        processingId: UUID,
        command: JobSummaryGenerateCommand
    ) {
        val brand =
            brandWriteService.getOrCreate(
                name = llmResult.brandName,
                companyId = null,
                source = BrandSource.INFERRED
            )

        // llmResult.positionName은 LLM이 후보군에서 선택한 이름
        val normalizedPositionName =
            Normalizer.normalizePosition(llmResult.positionName)

        val position: Position =
            positionCommand.findByNormalizedName(normalizedPositionName)
                ?: positionCommand.findByNormalizedName(
                    Normalizer.normalizePosition(UNKNOWN_POSITION_NAME)
                )
                ?: throw IllegalStateException("UNKNOWN position not found")

        val resolvedBrandPositionName =
            llmResult.brandPositionName?.takeIf { it.isNotBlank() }
                ?: command.positionName

        // command에 입력된 데이터 positionName는 사용자가 입력한 데이터 즉 BrandPositionName
        val brandPosition = brandPositionWriteService.getOrCreate(
            brandId = brand.id,
            positionId = position.id,
            displayName = resolvedBrandPositionName,
            source = BrandPositionSource.LLM
        )

        // 단일 트랜잭션: JobSummary + Outbox + Processing 완료
        val summary = summaryCreationService.createWithOutbox(
            processingId = processingId,
            snapshotId = snapshotId,
            brand = brand,
            positionId = position.id,
            positionName = position.name,
            brandPositionId = brandPosition.id,
            positionCategoryId = position.category.id,
            positionCategoryName = position.category.name,
            llmResult = llmResult,
            brandPositionName = resolvedBrandPositionName, // llmResult.brandPositionName이 없으면 사용자가 입력한 positionName으로
            sourceUrl = command.sourceUrl
        )

        llmResult.companyCandidate?.let {
            runCatching {
                companyCandidateWriteService.createCandidate(
                    summary.id,
                    brand.id,
                    it,
                    CompanyCandidateSource.LLM,
                    0.7
                )
            }
        }
    }
}
