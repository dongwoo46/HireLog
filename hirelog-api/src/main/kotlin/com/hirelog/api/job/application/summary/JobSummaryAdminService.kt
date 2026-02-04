package com.hirelog.api.job.application.summary

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.hirelog.api.brand.application.command.BrandWriteService
import com.hirelog.api.brand.domain.BrandSource
import com.hirelog.api.brandposition.application.BrandPositionWriteService
import com.hirelog.api.brandposition.domain.BrandPositionSource
import com.hirelog.api.common.application.outbox.OutboxEventWriteService
import com.hirelog.api.common.domain.outbox.AggregateType
import com.hirelog.api.common.domain.outbox.OutboxEvent
import com.hirelog.api.common.logging.log
import com.hirelog.api.common.utils.Normalizer
import com.hirelog.api.company.application.CompanyCandidateWriteService
import com.hirelog.api.company.application.port.CompanyQuery
import com.hirelog.api.company.domain.CompanyCandidateSource
import com.hirelog.api.job.application.intake.JdIntakePolicy
import com.hirelog.api.job.application.snapshot.JobSnapshotWriteService
import com.hirelog.api.job.application.snapshot.command.JobSnapshotCreateCommand
import com.hirelog.api.job.application.summary.payload.JobSummaryOutboxPayload
import com.hirelog.api.job.application.summary.port.JobSummaryLlm
import com.hirelog.api.job.application.summary.view.JobSummaryLlmResult
import com.hirelog.api.job.domain.JobSourceType
import com.hirelog.api.job.domain.JobSummary
import com.hirelog.api.job.domain.RecruitmentPeriodType
import com.hirelog.api.position.application.port.PositionQuery
import com.hirelog.api.position.application.view.PositionSummaryView
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
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
 * - 중복 체크 스킵 (Admin 판단 하에 강제 생성)
 * - 동기 처리 (비동기 파이프라인 미사용)
 */
@Service
class JobSummaryAdminService(
    private val jdIntakePolicy: JdIntakePolicy,
    private val snapshotWriteService: JobSnapshotWriteService,
    private val llmClient: JobSummaryLlm,
    private val summaryWriteService: JobSummaryWriteService,
    private val brandWriteService: BrandWriteService,
    private val brandPositionWriteService: BrandPositionWriteService,
    private val positionQuery: PositionQuery,
    private val outboxEventWriteService: OutboxEventWriteService,
    private val companyCandidateWriteService: CompanyCandidateWriteService,
    private val companyQuery: CompanyQuery,

    @Value("\${admin.verify.password}")
    private val adminVerifyPassword: String
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
     * 1. JD 텍스트 → 간단한 canonicalMap 변환
     * 2. Hash 계산 + Snapshot 저장
     * 3. Gemini 동기 호출
     * 4. JobSummary 저장
     *
     * @return 생성된 JobSummary ID
     */
    @Transactional
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

        // === 2. Hash 계산 ===
        val hashes = jdIntakePolicy.generateIntakeHashes(canonicalMap)

        // === 3. Snapshot 저장 ===
        val snapshotId = snapshotWriteService.record(
            JobSnapshotCreateCommand(
                sourceType = JobSourceType.TEXT,
                sourceUrl = sourceUrl,
                canonicalMap = canonicalMap,
                coreText = hashes.coreText,
                recruitmentPeriodType = RecruitmentPeriodType.UNKNOWN,
                openedDate = null,
                closedDate = null,
                canonicalHash = hashes.canonicalHash,
                simHash = hashes.simHash
            )
        )

        // === 4. Position 후보 + Company 목록 조회 ===
        val positionCandidates = positionQuery.findActiveNames()
        val existCompanies = companyQuery.findAllNames().map { it.name }

        // === 5. Gemini 동기 호출 ===
        val llmResult = llmClient
            .summarizeJobDescriptionAsync(
                brandName = brandName,
                positionName = positionName,
                positionCandidates = positionCandidates,
                existCompanies = existCompanies,
                canonicalMap = canonicalMap
            )
            .get(LLM_TIMEOUT_SECONDS, TimeUnit.SECONDS)

        // === 6. Post-LLM 처리 ===
        val summary = executePostLlm(
            snapshotId = snapshotId,
            llmResult = llmResult,
            inputPositionName = positionName,
            sourceUrl = sourceUrl
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

    private fun executePostLlm(
        snapshotId: Long,
        llmResult: JobSummaryLlmResult,
        inputPositionName: String,
        sourceUrl: String?
    ): JobSummary {

        val brand = brandWriteService.getOrCreate(
            name = llmResult.brandName,
            normalizedName = Normalizer.normalizeBrand(llmResult.brandName),
            companyId = null,
            source = BrandSource.INFERRED
        )

        val normalizedPositionName = Normalizer.normalizePosition(llmResult.positionName)

        val position: PositionSummaryView =
            positionQuery.findByNormalizedName(normalizedPositionName)
                ?: positionQuery.findByNormalizedName(
                    Normalizer.normalizePosition(UNKNOWN_POSITION_NAME)
                )
                ?: throw IllegalStateException("UNKNOWN position not found")

        val brandPosition = brandPositionWriteService.getOrCreate(
            brandId = brand.id,
            positionId = position.id,
            displayName = inputPositionName,
            source = BrandPositionSource.LLM
        )

        val summary = summaryWriteService.save(
            snapshotId = snapshotId,
            brand = brand,
            positionId = position.id,
            positionName = position.name,
            brandPositionId = brandPosition.id,
            positionCategoryId = position.categoryId,
            positionCategoryName = position.categoryName,
            llmResult = llmResult,
            brandPositionName = inputPositionName,
            sourceUrl = sourceUrl
        )

        outboxEventWriteService.append(
            OutboxEvent.occurred(
                aggregateType = AggregateType.JOB_SUMMARY,
                aggregateId = summary.id.toString(),
                eventType = JobSummaryOutboxConstants.EventType.CREATED,
                payload = buildEventPayload(summary)
            )
        )

        tryCreateCompanyCandidate(
            jdSummaryId = summary.id,
            brandId = brand.id,
            companyCandidate = llmResult.companyCandidate
        )

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
        } catch (e: Exception) {
            log.warn(
                "[ADMIN_COMPANY_CANDIDATE_FAILED] jdSummaryId={}, error={}",
                jdSummaryId, e.message
            )
        }
    }

    private fun buildEventPayload(summary: JobSummary): String {
        val payload = JobSummaryOutboxPayload.from(summary)
        return objectMapper.writeValueAsString(payload)
    }

    private val objectMapper = ObjectMapper().apply {
        registerKotlinModule()
        registerModule(JavaTimeModule())
        disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }
}
