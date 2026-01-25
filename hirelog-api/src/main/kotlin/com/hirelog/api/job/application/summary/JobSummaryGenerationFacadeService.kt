package com.hirelog.api.job.application.summary

import com.hirelog.api.brand.application.command.BrandWriteService
import com.hirelog.api.brand.domain.BrandSource
import com.hirelog.api.brandposition.application.BrandPositionWriteService
import com.hirelog.api.brandposition.domain.BrandPositionSource
import com.hirelog.api.common.exception.GeminiCallException
import com.hirelog.api.common.exception.GeminiParseException
import com.hirelog.api.common.logging.log
import com.hirelog.api.common.utils.Normalizer
import com.hirelog.api.job.application.intake.JdIntakePolicy
import com.hirelog.api.job.application.intake.model.DuplicateDecision
import com.hirelog.api.job.application.jobsummaryprocessing.JdSummaryProcessingWriteService
import com.hirelog.api.job.application.messaging.JdPreprocessResponseMessage
import com.hirelog.api.job.application.snapshot.JobSnapshotWriteService
import com.hirelog.api.job.application.snapshot.command.JobSnapshotCreateCommand
import com.hirelog.api.job.application.summary.port.JobSummaryLlm
import com.hirelog.api.job.application.summary.view.JobSummaryLlmResult
import com.hirelog.api.position.application.port.PositionQuery
import com.hirelog.api.position.application.query.PositionView
import org.springframework.stereotype.Service
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * JD 요약 비동기 파이프라인 오케스트레이터
 *
 * ┌─────────────────────────────────────────────────────────────────────────┐
 * │ 파이프라인 3단계                                                         │
 * │                                                                         │
 * │ [Pre-LLM]  호출 스레드(pipelineExecutor)에서 동기 실행                     │
 * │  → 유효성 검증 → 해시 생성 → 중복 판정 → 스냅샷 기록 → 상태 전이          │
 * │                                                                         │
 * │ [LLM]  NIO 스레드에서 비동기 실행 (WebClient, 호출 스레드 비차단)          │
 * │  → Gemini API 호출 → 응답 파싱 → 도메인 모델 조립                        │
 * │                                                                         │
 * │ [Post-LLM]  postProcessExecutor에서 실행 (NIO 스레드 오염 방지)           │
 * │  → Brand/Position/Summary 저장 → 완료 상태 전이                          │
 * └─────────────────────────────────────────────────────────────────────────┘
 *
 * Future 완료 의미:
 * - 정상 완료(null): 메시지 처리 완결 (성공/비즈니스 실패 모두 포함) → Consumer가 ACK
 * - 예외 완료: 인프라 장애 (DB 불가 등) → Consumer가 ACK하지 않음 → XPENDING 재처리
 */
@Service
class JobSummaryGenerationFacadeService(
    private val processingWriteService: JdSummaryProcessingWriteService,
    private val snapshotWriteService: JobSnapshotWriteService,
    private val jdIntakePolicy: JdIntakePolicy,
    private val llmClient: JobSummaryLlm,
    private val summaryWriteService: JobSummaryWriteService,
    private val brandWriteService: BrandWriteService,
    private val brandPositionWriteService: BrandPositionWriteService,
    private val positionQuery: PositionQuery,
) {

    companion object {
        private const val LLM_TIMEOUT_SECONDS = 45L
        private const val UNKNOWN_POSITION_NAME = "UNKNOWN"
    }

    /**
     * 비동기 JD 요약 파이프라인 진입점
     *
     * 이 메서드가 반환하는 CompletableFuture의 완료 시점이
     * Redis Stream ACK의 실행 시점을 결정한다.
     *
     * @param message 전처리 결과 메시지 (Redis Stream에서 수신)
     * @param postProcessExecutor Post-LLM DB 작업 실행용 Executor (MDC 전파 보장 필수)
     */
    fun generateAsync(
        message: JdPreprocessResponseMessage,
        postProcessExecutor: Executor
    ): CompletableFuture<Void> {

        log.info("[JD_SUMMARY_PIPELINE_START] requestId={}", message.requestId)
        // Processing 레코드 생성: 모든 요청에 대한 추적 이력 보장
        val processing = processingWriteService.startProcessing(
            requestId = message.requestId
        )

        // ── Pre-LLM Phase ───────────────────────────────────────────────────
        // 호출 스레드(pipelineExecutor)에서 동기 실행.
        // LLM 호출 전 게이트 역할: 유효하지 않거나 중복인 메시지를 조기 종결한다.
        val snapshotId: Long
        val positionCandidates: List<String>

        try {
            // 최소 요건(섹션 수, 텍스트 길이) 미충족 시 조기 종결
            if (!jdIntakePolicy.isValidJd(message)) {
                processingWriteService.markFailed(
                    processingId = processing.id,
                    errorCode = "INVALID_INPUT",
                    errorMessage = "JD 유효성 검증 실패: 최소 요건 미충족"
                )
                return CompletableFuture.completedFuture(null)
            }

            // canonicalMap 기반 해시 생성 (canonicalHash, simHash, coreText)
            val hashes = jdIntakePolicy.generateIntakeHashes(message.canonicalMap)

            // 해시 기반 중복 판정 (정확 일치 / 유사도 기반)
            val decision = jdIntakePolicy.decideDuplicate(message, hashes)

            // 중복이면 LLM 호출 없이 종결
            if (decision != DuplicateDecision.NOT_DUPLICATE) {
                processingWriteService.markDuplicate(processing.id, decision.name)
                return CompletableFuture.completedFuture(null)
            }

            // 스냅샷은 중복 여부와 무관하게 항상 기록 (수집 로그 성격)
            snapshotId = snapshotWriteService.record(
                JobSnapshotCreateCommand(
                    sourceType = message.source,
                    sourceUrl = message.sourceUrl,
                    canonicalMap = message.canonicalMap,
                    coreText = hashes.coreText,
                    recruitmentPeriodType = message.recruitmentPeriodType,
                    openedDate = message.openedDate,
                    closedDate = message.closedDate,
                    canonicalHash = hashes.canonicalHash,
                    simHash = hashes.simHash
                )
            )

            // LLM 요약 진입 상태 기록 (RECEIVED → SUMMARIZING)
            processingWriteService.markSummarizing(processing.id)

            // LLM 프롬프트에 전달할 Position 후보 목록 조회
            positionCandidates = positionQuery.findActive().map { it.name }

        } catch (e: Exception) {
            // Pre-LLM 인프라 장애 시 실패 기록 후 정상 종결 (ACK 대상)
            handleFailure(processing.id, e, message.requestId, "PRE_LLM")
            return CompletableFuture.completedFuture(null)
        }

        // ── LLM Phase ───────────────────────────────────────────────────────
        // WebClient Mono → CompletableFuture 변환.
        // 이 시점에서 호출 스레드(pipelineExecutor)는 즉시 반환된다.
        // 실제 HTTP I/O는 Netty NIO 스레드에서 비동기 처리된다.
        return llmClient.summarizeJobDescriptionAsync(
            brandName = message.brandName,
            positionName = message.positionName,
            positionCandidates = positionCandidates,
            canonicalMap = message.canonicalMap
        )
            .orTimeout(LLM_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            // ── Post-LLM Phase ──────────────────────────────────────────────
            // postProcessExecutor에서 실행: NIO 스레드에서 JPA/DB 작업 방지.
            // LLM 결과 기반 도메인 객체 생성 및 영속화.
            .thenAcceptAsync({ llmResult ->
                log.info(
                    "[JD_SUMMARY_LLM_SUCCESS] processingId={}, llmResult={}",
                    processing.id,
                    llmResult
                )
                executePostLlmPhase(snapshotId, llmResult, processing.id, message)
                processingWriteService.markCompleted(processing.id)

                log.info(
                    "[JD_SUMMARY_COMPLETED] requestId={}, processingId={}",
                    message.requestId, processing.id
                )
            }, postProcessExecutor)

            // ── 에러 종결 ───────────────────────────────────────────────────
            // LLM 호출 실패 또는 Post-LLM DB 실패 모두 이 블록에서 종결.
            // postProcessExecutor에서 실행: markFailed가 DB 접근하므로 NIO 스레드 불가.
            // null 반환 → Future 정상 완료로 전환 → Consumer가 ACK 수행.
            .exceptionallyAsync({ ex ->
                handleFailure(processing.id, ex, message.requestId, "LLM_OR_POST_LLM")
                null
            }, postProcessExecutor)
    }

    /**
     * Post-LLM Phase: LLM 결과를 기반으로 도메인 객체를 생성/연결하고 요약을 저장한다.
     *
     * 실행 스레드: postProcessExecutor (MdcExecutor 래핑)
     * 트랜잭션: 각 WriteService가 자체 @Transactional을 보유 (Facade 무트랜잭션)
     */
    private fun executePostLlmPhase(
        snapshotId: Long,
        llmResult: JobSummaryLlmResult,
        processingId: UUID,
        message: JdPreprocessResponseMessage
    ) {
        // Brand 확보: LLM이 판정한 회사명으로 조회 또는 신규 생성
        val brand = brandWriteService.getOrCreate(
            name = llmResult.brandName,
            normalizedName = Normalizer.normalizeBrand(llmResult.brandName),
            companyId = null,
            source = BrandSource.INFERRED
        )

        // Position 조회: LLM이 선택한 표준 포지션명으로 조회
        val normalizedPositionName = Normalizer.normalizePosition(llmResult.positionName)
        val position: PositionView = positionQuery.findByNormalizedName(normalizedPositionName)
            ?: run {
                log.warn(
                    "[POSITION_NOT_FOUND] requestId={}, processingId={}, llmPositionName={}, normalizedName={}, fallback={}",
                    message.requestId,
                    processingId,
                    llmResult.positionName,
                    normalizedPositionName,
                    UNKNOWN_POSITION_NAME
                )
                positionQuery.findByNormalizedName(Normalizer.normalizePosition(UNKNOWN_POSITION_NAME))
                    ?: throw IllegalStateException("UNKNOWN position not found in database")
            }

        // BrandPosition 연결: Brand-Position 매핑 (displayName = 회사 내부 포지션명)
        brandPositionWriteService.getOrCreate(
            brandId = brand.id,
            positionId = position.id,
//            displayName = llmResult.brandPositionName,
            displayName = message.positionName,
            source = BrandPositionSource.LLM
        )

        // JobSummary 영속화: 스냅샷 + Brand + Position + LLM 요약 결과 결합
        summaryWriteService.save(
            snapshotId = snapshotId,
            brand = brand,
            positionId = position.id,
            positionName = position.name,
            llmResult = llmResult
        )
    }

    /**
     * 파이프라인 실패를 종결한다.
     *
     * CompletableFuture 체인에서 발생한 예외는 CompletionException으로 래핑되므로
     * 원인 예외를 unwrap한 뒤 에러 코드를 분류한다.
     *
     * 호출 후 Processing 상태: FAILED (이후 상태 전이 불가)
     */
    private fun handleFailure(
        processingId: UUID,
        throwable: Throwable,
        requestId: String,
        phase: String
    ) {
        val cause = unwrap(throwable)

        val errorCode = when (cause) {
            is GeminiCallException -> "LLM_CALL_FAILED"
            is GeminiParseException -> "LLM_PARSE_FAILED"
            is TimeoutException -> "LLM_TIMEOUT"
            else -> "FAILED_AT_$phase"
        }
        val rootCause = generateSequence<Throwable>(cause) { it.cause }.last()
        val errorMessage = "[${cause.javaClass.simpleName}] ${rootCause.message ?: "Unknown error"}"

        log.error(
            "[JD_SUMMARY_FAILED] requestId={}, processingId={}, phase={}, errorCode={}, message={}",
            requestId, processingId, phase, errorCode, errorMessage, cause
        )

        processingWriteService.markFailed(
            processingId = processingId,
            errorCode = errorCode,
            errorMessage = errorMessage
        )
    }

    /**
     * CompletionException 언래핑.
     * CompletableFuture 체인에서 발생한 예외는 CompletionException으로 감싸지므로
     * 실제 원인 예외를 추출해야 정확한 에러 분류가 가능하다.
     */
    private fun unwrap(ex: Throwable): Throwable =
        if (ex is CompletionException) ex.cause ?: ex else ex
}
