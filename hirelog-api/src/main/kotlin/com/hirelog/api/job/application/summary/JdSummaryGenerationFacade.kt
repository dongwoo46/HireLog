package com.hirelog.api.job.application.summary.pipeline

import com.fasterxml.jackson.databind.ObjectMapper
import com.hirelog.api.brand.application.BrandWriteService
import com.hirelog.api.brand.domain.BrandSource
import com.hirelog.api.relation.application.brandposition.BrandPositionWriteService
import com.hirelog.api.relation.domain.type.BrandPositionSource
import com.hirelog.api.company.application.CompanyCandidateWriteService
import com.hirelog.api.company.application.port.CompanyQuery
import com.hirelog.api.company.domain.CompanyCandidateSource
import com.hirelog.api.common.exception.GeminiCallException
import com.hirelog.api.common.exception.GeminiParseException
import com.hirelog.api.common.logging.log
import com.hirelog.api.common.utils.Normalizer
import com.hirelog.api.job.application.intake.JdIntakePolicy
import com.hirelog.api.job.application.intake.model.DuplicateDecision
import com.hirelog.api.job.application.jobsummaryprocessing.JdSummaryProcessingWriteService
import com.hirelog.api.job.application.snapshot.JobSnapshotWriteService
import com.hirelog.api.job.application.snapshot.command.JobSnapshotCreateCommand
import com.hirelog.api.job.application.summary.JobSummaryCreationService
import com.hirelog.api.job.application.summary.command.JobSummaryGenerateCommand
import com.hirelog.api.job.application.summary.port.JobSummaryLlm
import com.hirelog.api.job.application.summary.port.JobSummaryQuery
import com.hirelog.api.job.application.summary.view.JobSummaryLlmResult
import com.hirelog.api.job.domain.JobSourceType
import com.hirelog.api.position.application.port.PositionCommand
import com.hirelog.api.position.application.port.PositionQuery
import com.hirelog.api.position.domain.Position
import org.springframework.stereotype.Service
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * SummaryGenerationPipeline
 *
 * 책임:
 * - JD 요약 생성 유스케이스의 순수 비즈니스 파이프라인 실행
 *
 * 설계 원칙:
 * - Kafka / Redis / ACK / Executor 개념 ❌
 * - Application Command만 입력으로 받는다
 */
@Service
class JdSummaryGenerationFacade(
    private val processingWriteService: JdSummaryProcessingWriteService,
    private val snapshotWriteService: JobSnapshotWriteService,
    private val jdIntakePolicy: JdIntakePolicy,
    private val llmClient: JobSummaryLlm,
    private val summaryCreationService: JobSummaryCreationService,
    private val summaryQuery: JobSummaryQuery,
    private val brandWriteService: BrandWriteService,
    private val brandPositionWriteService: BrandPositionWriteService,
    private val positionQuery: PositionQuery,
    private val companyCandidateWriteService: CompanyCandidateWriteService,
    private val companyQuery: CompanyQuery,
    private val objectMapper: ObjectMapper,
    private val positionCommand: PositionCommand
) {

    companion object {
        private const val LLM_TIMEOUT_SECONDS = 45L
        private const val UNKNOWN_POSITION_NAME = "UNKNOWN"
        private const val LLM_COMPANY_CONFIDENCE_SCORE = 0.7
    }

    /**
     * JD 요약 파이프라인 실행
     *
     * @return
     * - 정상 완료: completedFuture(null)
     * - 인프라 장애: exceptionally 완료
     */
    fun execute(command: JobSummaryGenerateCommand): CompletableFuture<Void> {

        val processing =
            processingWriteService.startProcessing(command.requestId)

        val snapshotId: Long
        val positionCandidates: List<String>
        val existCompanies: List<String>

        log.info("[JD_SUMMARY_PIPELINE_STARTED] JobSummaryGenerateCommand={}", command)

        // ── Pre-LLM Phase ─────────────────────────────────────────────
        try {
            if (!jdIntakePolicy.isValidJd(command)) {
                processingWriteService.markFailed(
                    processingId = processing.id,
                    errorCode = "INVALID_INPUT",
                    errorMessage = "JD 유효성 검증 실패"
                )
                return CompletableFuture.completedFuture(null)
            }

            if (command.source == JobSourceType.URL && command.sourceUrl != null) {
                if (summaryQuery.existsBySourceUrl(command.sourceUrl)) {
                    processingWriteService.markDuplicate(
                        processingId = processing.id,
                        reason = "URL_DUPLICATE"
                    )
                    return CompletableFuture.completedFuture(null)
                }
            }

            val hashes = jdIntakePolicy.generateIntakeHashes(command.canonicalMap)
            val decision = jdIntakePolicy.decideDuplicate(command, hashes)

            if (decision is DuplicateDecision.Duplicate) {
                processingWriteService.markDuplicate(
                    processingId = processing.id,
                    reason = decision.reason.name
                )
                log.info(
                    "[JD_DUPLICATE_DETECTED] reason={}, existingSnapshotId={}, existingSummaryId={}",
                    decision.reason, decision.existingSnapshotId, decision.existingSummaryId
                )
                return CompletableFuture.completedFuture(null)
            }

            snapshotId =
                snapshotWriteService.record(
                    JobSnapshotCreateCommand(
                        sourceType = command.source,
                        sourceUrl = command.sourceUrl,
                        canonicalMap = command.canonicalMap,
                        coreText = hashes.coreText,
                        recruitmentPeriodType = command.recruitmentPeriodType,
                        openedDate = command.openedDate,
                        closedDate = command.closedDate,
                        canonicalHash = hashes.canonicalHash,
                        simHash = hashes.simHash
                    )
                )

            processingWriteService.markSummarizing(processing.id, snapshotId)

            positionCandidates =
                positionQuery.findActiveNames()

            existCompanies =
                companyQuery.findAllNames().map { it.name }

        } catch (e: Exception) {
            handleFailure(processing.id, e, command.requestId, "PRE_LLM")
            return CompletableFuture.completedFuture(null)
        }

        // ── LLM Phase ────────────────────────────────────────────────
        return llmClient
            .summarizeJobDescriptionAsync(
                brandName = command.brandName,
                positionName = command.positionName,
                positionCandidates = positionCandidates,
                existCompanies = existCompanies,
                canonicalMap = command.canonicalMap
            )
            .orTimeout(LLM_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .thenAccept { llmResult ->
                // LLM 결과 임시 저장 (별도 트랜잭션 - 복구용)
                val llmResultJson = objectMapper.writeValueAsString(llmResult)
                processingWriteService.saveLlmResult(
                    processingId = processing.id,
                    llmResultJson = llmResultJson,
                    commandBrandName = command.brandName,
                    commandPositionName = command.positionName // 사용자가 입력한 position명 = brandPosition
                )

                // Post-LLM 처리 (단일 트랜잭션: Summary + Outbox + Processing 완료)
                executePostLlm(snapshotId, llmResult, processing.id, command)
            }
            .exceptionally { ex ->
                handleFailure(processing.id, ex, command.requestId, "LLM_OR_POST_LLM")
                null
            }
    }

    // ── Post-LLM Phase ─────────────────────────────────────────────
    // brandPositionName은 사용자가 실제로 입력한 이름 / positionName은 llm을 통해 position 후보중 선택된 이름
    private fun executePostLlm(
        snapshotId: Long,
        llmResult: JobSummaryLlmResult,
        processingId: UUID,
        command: JobSummaryGenerateCommand // command.positionName = brandPositionName
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

        // CompanyCandidate 생성 (비필수 - 실패해도 파이프라인에 영향 없음)
        tryCreateCompanyCandidate(
            jdSummaryId = summary.id,
            brandId = brand.id,
            companyCandidate = llmResult.companyCandidate
        )
    }

    /**
     * CompanyCandidate 생성 시도
     *
     * 정책:
     * - LLM이 추론한 companyCandidate가 있는 경우에만 생성
     * - 실패해도 파이프라인 전체에 영향 없음
     */
    private fun tryCreateCompanyCandidate(
        jdSummaryId: Long,
        brandId: Long,
        companyCandidate: String?
    ) {
        if (companyCandidate.isNullOrBlank()) {
            return
        }

        try {
            companyCandidateWriteService.createCandidate(
                jdSummaryId = jdSummaryId,
                brandId = brandId,
                candidateName = companyCandidate,
                source = CompanyCandidateSource.LLM,
                confidenceScore = LLM_COMPANY_CONFIDENCE_SCORE
            )
            log.info("[COMPANY_CANDIDATE_CREATED] jdSummaryId={}, brandId={}, companyCandidate={}",
                jdSummaryId, brandId, companyCandidate)
        } catch (e: Exception) {
            log.warn("[COMPANY_CANDIDATE_FAILED] jdSummaryId={}, brandId={}, companyCandidate={}, error={}",
                jdSummaryId, brandId, companyCandidate, e.message)
        }
    }

    // ── Error Handling ─────────────────────────────────────────────
    private fun handleFailure(
        processingId: UUID,
        throwable: Throwable,
        requestId: String,
        phase: String
    ) {
        val cause = unwrap(throwable)

        val errorCode =
            when (cause) {
                is GeminiCallException -> "LLM_CALL_FAILED"
                is GeminiParseException -> "LLM_PARSE_FAILED"
                is TimeoutException -> "LLM_TIMEOUT"
                else -> "FAILED_AT_$phase"
            }

        log.error(
            "[JD_PIPELINE_FAILED] requestId={}, phase={}, errorCode={}, message={}",
            requestId, phase, errorCode, cause.message, cause
        )

        processingWriteService.markFailed(
            processingId = processingId,
            errorCode = errorCode,
            errorMessage = cause.message ?: "Unknown error"
        )
    }

    private fun unwrap(ex: Throwable): Throwable =
        if (ex is CompletionException) ex.cause ?: ex else ex
}
