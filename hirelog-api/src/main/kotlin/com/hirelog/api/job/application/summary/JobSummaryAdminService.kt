package com.hirelog.api.job.application.summary

import com.hirelog.api.common.logging.log
import com.hirelog.api.job.application.intake.JdIntakePolicy
import com.hirelog.api.job.application.intake.model.DuplicateDecision
import com.hirelog.api.job.application.snapshot.port.JobSnapshotCommand
import com.hirelog.api.job.application.summary.pipeline.PostLlmProcessor
import com.hirelog.api.job.application.summary.payload.JobSummaryOutboxPayload
import com.hirelog.api.job.application.summary.payload.JobSummarySearchPayload
import com.hirelog.api.job.application.summary.port.AdminJdUrlFetchPort
import com.hirelog.api.job.application.summary.port.AdminJdUrlFetchPort.AdminJdFetchResult
import com.hirelog.api.job.application.summary.port.JobSummaryCommand
import com.hirelog.api.job.application.summary.port.JobSummaryEmbedding
import com.hirelog.api.job.application.summary.port.JobSummaryLlm
import com.hirelog.api.job.application.summary.port.JobSummaryQuery
import com.hirelog.api.job.domain.model.JobSnapshot
import com.hirelog.api.job.domain.type.JobSourceType
import com.hirelog.api.job.domain.type.RecruitmentPeriodType
import com.hirelog.api.company.application.port.CompanyQuery
import com.hirelog.api.position.application.port.PositionQuery
import com.hirelog.api.job.infra.persistence.opensearch.JobSummaryIndexManager
import com.hirelog.api.job.infra.persistence.opensearch.JobSummaryOpenSearchAdapter
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.nio.file.AccessDeniedException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Admin 전용 JobSummary 생성 서비스
 *
 * 책임:
 * - Python 전처리 파이프라인 없이 직접 Gemini 호출
 * - 수동 데이터 처리용
 *
 * 정책:
 * - 중복 체크 후 LLM 호출
 * - 동기 처리 (비동기 파이프라인 미사용)
 * - Snapshot + JobSummary + Outbox 단일 트랜잭션 저장
 */
@Service
class JobSummaryAdminService(
    private val jdIntakePolicy: JdIntakePolicy,
    private val snapshotCommand: JobSnapshotCommand,
    @Qualifier("geminiJobSummaryLlm")
    private val llmClient: JobSummaryLlm,
    private val summaryQuery: JobSummaryQuery,
    private val positionQuery: PositionQuery,
    private val companyQuery: CompanyQuery,
    private val postLlmProcessor: PostLlmProcessor,
    private val openSearchAdapter: JobSummaryOpenSearchAdapter,
    private val indexManager: JobSummaryIndexManager,
    private val summaryCommand: JobSummaryCommand,
    private val embeddingPort: JobSummaryEmbedding,
    private val adminJdUrlFetchPort: AdminJdUrlFetchPort,
    @Value("\${admin.verify.password}")
    private val adminVerifyPassword: String
) {

    companion object {
        private const val LLM_TIMEOUT_SECONDS = 60L
    }

    /**
     * Admin 전용 JobSummary 직접 생성
     *
     * 처리 흐름:
     * 1. JD 텍스트 → canonicalMap 변환
     * 2. 중복 체크 (sourceUrl 기준)
     * 3. Hash 계산 (메모리)
     * 4. Position 후보 + Company 목록 조회 (읽기 전용)
     * 5. Gemini 동기 호출 (DB 커넥션 점유 없음)
     * 6. PostLlmProcessor 위임: Brand/Position 해석 + 단일 트랜잭션 저장
     *
     * @return 생성된 JobSummary ID
     */
    fun createDirectly(
        brandName: String,
        positionName: String,
        jdText: String,
        sourceUrl: String?
    ): Long {

        log.info(
            "[ADMIN_JOB_SUMMARY_CREATE_START] brandName={}, positionName={}, jdTextLength={}",
            brandName, positionName, jdText.length
        )

        // === 1. JD 텍스트 → canonicalMap 변환 ===
        val canonicalMap = buildAdminCanonicalMap(jdText)

        // === 2. 중복 체크 (sourceUrl 기준) ===
        if (sourceUrl != null && summaryQuery.existsBySourceUrl(sourceUrl)) {
            throw IllegalStateException("Duplicate JD: sourceUrl already exists. url=$sourceUrl")
        }

        // === 3. Hash 계산 + 중복/Reprocessable 판정 ===
        val hashes = jdIntakePolicy.generateIntakeHashes(canonicalMap)
        val hashDecision = jdIntakePolicy.findHashDuplicate(hashes.canonicalHash)

        when (hashDecision) {
            is DuplicateDecision.Duplicate -> {
                throw IllegalStateException(
                    "Duplicate JD: hash duplicate. snapshotId=${hashDecision.existingSnapshotId}, summaryId=${hashDecision.existingSummaryId}"
                )
            }
            is DuplicateDecision.Reprocessable -> {
                log.info("[ADMIN_JD_REPROCESS] existingSnapshotId={}", hashDecision.existingSnapshotId)
            }
            else -> {}
        }

        // === 4. Position 후보 + Company 목록 조회 (읽기 전용) ===
        val positionCandidates = positionQuery.findActiveNames()
        val existCompanies = companyQuery.findAllNames().map { it.name }

        // === 5. Gemini 동기 호출 (DB 커넥션 점유 없음) ===
        val llmResult = try {
            llmClient
                .summarizeJobDescriptionAsync(
                    brandName = brandName,
                    positionName = positionName,
                    positionCandidates = positionCandidates,
                    existCompanies = existCompanies,
                    canonicalMap = canonicalMap
                )
                .get(LLM_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        } catch (e: TimeoutException) {
            log.error(
                "[ADMIN_LLM_TIMEOUT] brandName={}, positionName={}, timeoutSeconds={}",
                brandName, positionName, LLM_TIMEOUT_SECONDS, e
            )
            throw e
        }

        // === 6. PostLlmProcessor 위임 ===
        val snapshotSupplier = buildSnapshotCommand(hashDecision, canonicalMap, hashes, sourceUrl)

        val summary = postLlmProcessor.executeForAdmin(
            snapshotCommand = snapshotSupplier,
            llmResult = llmResult,
            brandPositionName = positionName,
            sourceUrl = sourceUrl
        )

        log.info(
            "[ADMIN_JOB_SUMMARY_CREATE_SUCCESS] summaryId={}, brandName={}, positionName={}",
            summary.id, llmResult.brandName, llmResult.positionName
        )

        return summary.id
    }

    private fun buildSnapshotCommand(
        hashDecision: DuplicateDecision?,
        canonicalMap: Map<String, List<String>>,
        hashes: com.hirelog.api.job.application.intake.model.IntakeHashes,
        sourceUrl: String?
    ): JobSummaryWriteService.JobSnapshotCommand {
        return if (hashDecision is DuplicateDecision.Reprocessable) {
            JobSummaryWriteService.JobSnapshotCommand {
                snapshotCommand.findById(hashDecision.existingSnapshotId)
                    ?: throw IllegalStateException("Snapshot not found. id=${hashDecision.existingSnapshotId}")
            }
        } else {
            JobSummaryWriteService.JobSnapshotCommand {
                val snapshot = JobSnapshot.create(
                    sourceType = JobSourceType.TEXT,
                    sourceUrl = sourceUrl,
                    canonicalSections = canonicalMap,
                    recruitmentPeriodType = RecruitmentPeriodType.UNKNOWN,
                    openedDate = null,
                    closedDate = null,
                    canonicalHash = hashes.canonicalHash,
                    simHash = hashes.simHash,
                    coreText = hashes.coreText
                )
                snapshotCommand.record(snapshot)
                snapshot
            }
        }
    }

    /**
     * URL 크롤링 기반 Admin JobSummary 생성
     *
     * 처리 흐름:
     * 1. Python 임베딩 서버 /admin/fetch-url 동기 호출
     * 2. SUCCESS → 텍스트로 createDirectly() 위임
     *    IMAGE_BASED → 이미지로 Gemini 멀티모달 호출 후 저장
     *    INSUFFICIENT / ERROR → 예외 전파
     *
     * @return 생성된 JobSummary ID
     */
    fun createFromUrl(
        brandName: String,
        positionName: String,
        url: String
    ): Long {
        log.info("[ADMIN_JOB_SUMMARY_URL_FETCH] brandName={}, url={}", brandName, url)

        return when (val fetchResult = adminJdUrlFetchPort.fetch(url)) {
            is AdminJdFetchResult.Success -> {
                createDirectly(
                    brandName = brandName,
                    positionName = positionName,
                    jdText = fetchResult.text,
                    sourceUrl = url
                )
            }

            is AdminJdFetchResult.ImageBased -> {
                createFromImages(
                    brandName = brandName,
                    positionName = positionName,
                    images = fetchResult.images,
                    sourceUrl = url
                )
            }

            is AdminJdFetchResult.Insufficient -> {
                throw IllegalStateException("JD 텍스트 추출 불충분: ${fetchResult.message}")
            }

            is AdminJdFetchResult.Error -> {
                throw IllegalStateException("URL 크롤링 실패: ${fetchResult.message}")
            }
        }
    }

    fun createDirectlyFromImages(
        brandName: String,
        positionName: String,
        images: List<String>,
        sourceUrl: String? = null
    ): Long = createFromImages(brandName, positionName, images, sourceUrl)

    private fun createFromImages(
        brandName: String,
        positionName: String,
        images: List<String>,
        sourceUrl: String?
    ): Long {
        if (sourceUrl != null && summaryQuery.existsBySourceUrl(sourceUrl)) {
            throw IllegalStateException("Duplicate JD: sourceUrl already exists. url=$sourceUrl")
        }

        val positionCandidates = positionQuery.findActiveNames()
        val existCompanies = companyQuery.findAllNames().map { it.name }

        val llmResult = try {
            llmClient.summarizeFromImagesAsync(
                brandName = brandName,
                positionName = positionName,
                positionCandidates = positionCandidates,
                existCompanies = existCompanies,
                images = images
            ).get(LLM_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        } catch (e: TimeoutException) {
            log.error(
                "[ADMIN_LLM_IMAGE_TIMEOUT] brandName={}, positionName={}, timeoutSeconds={}",
                brandName, positionName, LLM_TIMEOUT_SECONDS, e
            )
            throw e
        }

        val emptyCanonicalMap = mapOf("etc" to emptyList<String>())
        val hashes = jdIntakePolicy.generateIntakeHashes(emptyCanonicalMap)

        val snapshotSupplier = JobSummaryWriteService.JobSnapshotCommand {
            val snapshot = JobSnapshot.create(
                sourceType = JobSourceType.TEXT,
                sourceUrl = sourceUrl,
                canonicalSections = emptyCanonicalMap,
                recruitmentPeriodType = RecruitmentPeriodType.UNKNOWN,
                openedDate = null,
                closedDate = null,
                canonicalHash = hashes.canonicalHash,
                simHash = hashes.simHash,
                coreText = hashes.coreText
            )
            snapshotCommand.record(snapshot)
            snapshot
        }

        val summary = postLlmProcessor.executeForAdmin(
            snapshotCommand = snapshotSupplier,
            llmResult = llmResult,
            brandPositionName = positionName,
            sourceUrl = sourceUrl
        )

        log.info(
            "[ADMIN_JOB_SUMMARY_IMAGE_CREATE_SUCCESS] summaryId={}, brandName={}, positionName={}",
            summary.id, llmResult.brandName, llmResult.positionName
        )

        return summary.id
    }

    private fun buildAdminCanonicalMap(jdText: String): Map<String, List<String>> {
        val lines = jdText
            .split("\n")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        return mapOf("etc" to lines)
    }

    /**
     * 전체 재인덱싱
     *
     * 처리 흐름:
     * 1. 기존 OpenSearch 인덱스 삭제 + 재생성 (knn_vector 매핑 포함)
     * 2. DB에서 전체 JobSummary 커서 기반으로 순차 조회
     * 3. 각 문서 임베딩 + OpenSearch 인덱싱
     *
     * @param batchSize 한 번에 처리할 건수
     * @return 성공적으로 인덱싱된 문서 수
     */
    fun reindexAll(batchSize: Int = 50): Int {
        require(batchSize in 1..200) { "batchSize must be between 1 and 200" }

        log.info("[ADMIN_REINDEX_ALL_START] batchSize={}", batchSize)

        indexManager.deleteIndex()
        indexManager.createIndexIfNotExists()

        var lastId = 0L
        var totalSuccess = 0

        while (true) {
            val batch = summaryCommand.findAllForReindex(lastId, batchSize)
            if (batch.isEmpty()) break

            batch.forEach { summary ->
                runCatching {
                    val outbox = JobSummaryOutboxPayload.from(summary)
                    val searchPayload = JobSummarySearchPayload.from(outbox)

                    val vector = embeddingPort.embed(
                        JobSummaryEmbedding.EmbedRequest(
                            responsibilities = summary.responsibilities,
                            requiredQualifications = summary.requiredQualifications,
                            preferredQualifications = summary.preferredQualifications,
                            idealCandidate = summary.insight.idealCandidate,
                            mustHaveSignals = summary.insight.mustHaveSignals,
                            technicalContext = summary.insight.technicalContext
                        )
                    )

                    openSearchAdapter.index(searchPayload.copy(embeddingVector = vector))
                    totalSuccess++
                }.onFailure {
                    log.error(
                        "[ADMIN_REINDEX_ALL_DOC_FAILED] id={}, error={}",
                        summary.id, it.message
                    )
                }
            }

            lastId = batch.last().id
            log.info("[ADMIN_REINDEX_ALL_PROGRESS] lastId={}, totalSuccess={}", lastId, totalSuccess)

            if (batch.size < batchSize) break
        }

        log.info("[ADMIN_REINDEX_ALL_DONE] totalSuccess={}", totalSuccess)
        return totalSuccess
    }

    /**
     * 임베딩 벡터 누락 문서 재임베딩
     *
     * 처리 흐름:
     * 1. OpenSearch에서 embeddingVector 누락 문서 조회
     * 2. 각 문서에 대해 임베딩 서버 호출
     * 3. 벡터 부분 업데이트
     *
     * @param batchSize 한 번에 처리할 문서 수 (최대 500)
     * @return 성공적으로 재임베딩된 문서 수
     */
    fun reindexMissingEmbeddings(batchSize: Int): Int {
        require(batchSize in 1..500) { "batchSize must be between 1 and 500" }

        val candidates = openSearchAdapter.findMissingEmbedding(batchSize)

        log.info("[ADMIN_REINDEX_EMBEDDING_START] candidates={}", candidates.size)

        var successCount = 0

        candidates.forEach { candidate ->
            runCatching {
                val vector = embeddingPort.embed(
                    JobSummaryEmbedding.EmbedRequest(
                        responsibilities = candidate.responsibilities,
                        requiredQualifications = candidate.requiredQualifications,
                        preferredQualifications = candidate.preferredQualifications,
                        idealCandidate = candidate.idealCandidate,
                        mustHaveSignals = candidate.mustHaveSignals,
                        technicalContext = candidate.technicalContext
                    )
                )
                openSearchAdapter.updateEmbeddingVector(candidate.id, vector)
                successCount++
            }.onFailure {
                log.error(
                    "[ADMIN_REINDEX_EMBEDDING_FAILED] id={}, error={}",
                    candidate.id, it.message
                )
            }
        }

        log.info(
            "[ADMIN_REINDEX_EMBEDDING_DONE] total={}, success={}, failed={}",
            candidates.size, successCount, candidates.size - successCount
        )

        return successCount
    }

    fun verify(password: String) {
        if (password != adminVerifyPassword) {
            log.warn("[ADMIN_VERIFY_FAILED]")
            throw AccessDeniedException("Invalid admin verification password")
        }

        log.info("[ADMIN_VERIFY_SUCCESS]")
    }
}
