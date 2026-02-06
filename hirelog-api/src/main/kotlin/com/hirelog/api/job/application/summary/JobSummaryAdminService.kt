package com.hirelog.api.job.application.summary

import com.hirelog.api.brand.application.command.BrandWriteService
import com.hirelog.api.brand.domain.BrandSource
import com.hirelog.api.relation.application.brandposition.BrandPositionWriteService
import com.hirelog.api.relation.domain.type.BrandPositionSource
import com.hirelog.api.common.logging.log
import com.hirelog.api.common.utils.Normalizer
import com.hirelog.api.company.application.CompanyCandidateWriteService
import com.hirelog.api.company.application.port.CompanyQuery
import com.hirelog.api.company.domain.CompanyCandidateSource
import com.hirelog.api.job.application.intake.JdIntakePolicy
import com.hirelog.api.job.application.snapshot.port.JobSnapshotCommand
import com.hirelog.api.job.application.summary.port.JobSummaryLlm
import com.hirelog.api.job.application.summary.port.JobSummaryQuery
import com.hirelog.api.job.domain.JobSnapshot
import com.hirelog.api.job.domain.JobSourceType
import com.hirelog.api.job.domain.RecruitmentPeriodType
import com.hirelog.api.position.application.port.PositionCommand
import com.hirelog.api.position.application.port.PositionQuery
import com.hirelog.api.position.domain.Position
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
    private val llmClient: JobSummaryLlm,
    private val summaryCreationService: JobSummaryCreationService,
    private val summaryQuery: JobSummaryQuery,
    private val brandWriteService: BrandWriteService,
    private val brandPositionWriteService: BrandPositionWriteService,
    private val positionQuery: PositionQuery,
    private val companyCandidateWriteService: CompanyCandidateWriteService,
    private val companyQuery: CompanyQuery,
    @Value("\${admin.verify.password}")
    private val adminVerifyPassword: String,
    private val positionCommand: PositionCommand

) {

    companion object {
        private const val LLM_TIMEOUT_SECONDS = 60L
        private const val UNKNOWN_POSITION_NAME = "UNKNOWN"
        private const val LLM_COMPANY_CONFIDENCE_SCORE = 0.7
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
     * 6. 단일 트랜잭션: Snapshot + Brand/Position + JobSummary + Outbox 저장
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

        // === 3. Hash 계산 (메모리) ===
        val hashes = jdIntakePolicy.generateIntakeHashes(canonicalMap)

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

        // === 6. 단일 트랜잭션: 모든 데이터 저장 ===
        val brand = brandWriteService.getOrCreate(
            name = llmResult.brandName,
            normalizedName = Normalizer.normalizeBrand(llmResult.brandName),
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

        val brandPosition = brandPositionWriteService.getOrCreate(
            brandId = brand.id,
            positionId = position.id,
            displayName = positionName,
            source = BrandPositionSource.LLM
        )

        // Snapshot 생성 람다 (createAllForAdmin 내부에서 실행됨)
        val snapshotSupplier = JobSummaryCreationService.JobSnapshotCommand {
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

        val summary = summaryCreationService.createAllForAdmin(
            snapshotCommand = snapshotSupplier,
            llmResult = llmResult,
            brand = brand,
            positionId = position.id,
            positionName = position.name,
            brandPositionId = brandPosition.id,
            positionCategoryId = position.category.id,
            positionCategoryName = position.category.name,
            brandPositionName = positionName,
            sourceUrl = sourceUrl
        )

        // CompanyCandidate 생성 (비필수 - 실패해도 영향 없음)
        tryCreateCompanyCandidate(
            jdSummaryId = summary.id,
            brandId = brand.id,
            companyCandidate = llmResult.companyCandidate
        )

        log.info(
            "[ADMIN_JOB_SUMMARY_CREATE_SUCCESS] summaryId={}, brandName={}, positionName={}",
            summary.id, llmResult.brandName, llmResult.positionName
        )

        return summary.id
    }

    /**
     * Admin용 canonicalMap 생성
     *
     * 정책:
     * - 전처리 없이 원문 그대로 사용
     * - 섹션 구분 없이 raw 섹션에 전체 텍스트 저장
     * - LLM이 섹션 구분을 담당
     */
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

    /**
     * Admin 액션 수행 전 비밀번호 검증
     *
     * 정책:
     * - 설정된 admin verify password와 일치해야 함
     * - 실패 시 AccessDeniedException 발생
     */
    fun verify(password: String) {
        if (password != adminVerifyPassword) {
            log.warn("[ADMIN_VERIFY_FAILED]")
            throw AccessDeniedException("Invalid admin verification password")
        }

        log.info("[ADMIN_VERIFY_SUCCESS]")
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
        } catch (e: Exception) {
            log.warn(
                "[ADMIN_COMPANY_CANDIDATE_FAILED] jdSummaryId={}, error={}",
                jdSummaryId, e.message
            )
        }
    }
}
