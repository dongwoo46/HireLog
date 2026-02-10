package com.hirelog.api.job.application.summary

import com.hirelog.api.common.logging.log
import com.hirelog.api.job.application.intake.JdIntakePolicy
import com.hirelog.api.job.application.intake.model.DuplicateDecision
import com.hirelog.api.job.application.snapshot.port.JobSnapshotCommand
import com.hirelog.api.job.application.summary.pipeline.PostLlmProcessor
import com.hirelog.api.job.application.summary.port.JobSummaryLlm
import com.hirelog.api.job.application.summary.port.JobSummaryQuery
import com.hirelog.api.job.domain.model.JobSnapshot
import com.hirelog.api.job.domain.type.JobSourceType
import com.hirelog.api.job.domain.type.RecruitmentPeriodType
import com.hirelog.api.company.application.port.CompanyQuery
import com.hirelog.api.position.application.port.PositionQuery
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.nio.file.AccessDeniedException
import java.util.concurrent.TimeUnit

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
        val llmResult = llmClient
            .summarizeJobDescriptionAsync(
                brandName = brandName,
                positionName = positionName,
                positionCandidates = positionCandidates,
                existCompanies = existCompanies,
                canonicalMap = canonicalMap
            )
            .get(LLM_TIMEOUT_SECONDS, TimeUnit.SECONDS)

        // === 6. PostLlmProcessor 위임 ===
        val snapshotSupplier = buildSnapshotCommand(hashDecision, canonicalMap, hashes, sourceUrl)

        val summary = postLlmProcessor.executeForAdmin(
            snapshotCommand = snapshotSupplier,
            llmResult = llmResult,
            fallbackPositionName = positionName,
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

    private fun buildAdminCanonicalMap(jdText: String): Map<String, List<String>> {
        val lines = jdText
            .split("\n")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        return mapOf(
            "raw" to lines,
            "responsibilities" to lines,
            "requirements" to lines,
            "preferred" to lines
        )
    }

    fun verify(password: String) {
        if (password != adminVerifyPassword) {
            log.warn("[ADMIN_VERIFY_FAILED]")
            throw AccessDeniedException("Invalid admin verification password")
        }

        log.info("[ADMIN_VERIFY_SUCCESS]")
    }
}
