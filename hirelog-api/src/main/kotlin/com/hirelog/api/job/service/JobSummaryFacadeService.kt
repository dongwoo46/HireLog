package com.hirelog.api.job.service

import com.hirelog.api.common.exception.DuplicateJobSnapshotException
import com.hirelog.api.company.domain.Brand
import com.hirelog.api.company.domain.Position
import com.hirelog.api.company.service.BrandService
import com.hirelog.api.company.service.PositionService
import com.hirelog.api.job.domain.JobSnapshot
import com.hirelog.api.job.domain.JobSourceType
import com.hirelog.api.job.domain.JobSummary
import com.hirelog.api.job.dto.JobSummaryResult
import com.hirelog.api.job.repository.JobSnapshotRepository
import com.hirelog.api.job.repository.JobSummaryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest

@Service
class JobSummaryFacadeService(
    private val brandService: BrandService,
    private val positionService: PositionService,
    private val jobSnapshotRepository: JobSnapshotRepository,
    private val jobSummaryRepository: JobSummaryRepository,
    private val geminiService: GeminiService
) {

    /**
     * Facade
     * - 트랜잭션 ❌
     * - 외부 API 호출 ⭕
     * - 오케스트레이션 전용
     */
    fun summarizeTextJDAndSave(
        brandName: String,
        positionName: String,
        rawText: String
    ): JobSummaryResult {

        // 1️⃣ Brand
        val brand = brandService.getOrCreate(brandName)

        // 2️⃣ Position
        val position = positionService.getOrCreate(
            brandId = brand.id,
            positionName = positionName
        )

        // 3️⃣ canonicalText (임시)
        val canonicalText = canonicalize(rawText)

        // 4️⃣ contentHash
        val contentHash = sha256("${brand.id}|${position.id}|$canonicalText")

        // 5️⃣ 중복 체크 (낙관적)
        if (jobSnapshotRepository.existsByContentHash(contentHash)) {
            throw DuplicateJobSnapshotException()
        }

        // 6️⃣ Gemini 요약 (외부 API → 트랜잭션 밖)
        val summary = geminiService.summaryTextJobDescription(
            brandName = brand.name,
            position = position.name,
            jdText = rawText
        )

        // 7️⃣ DB 저장 (트랜잭션 전용 메서드)
        return saveSnapshotAndSummary(
            brand = brand,
            position = position,
            rawText = rawText,
            contentHash = contentHash,
            summary = summary
        )
    }

    /**
     * Transaction Boundary
     * - DB write 전용
     */
    @Transactional
    fun saveSnapshotAndSummary(
        brand: Brand,
        position: Position,
        rawText: String,
        contentHash: String,
        summary: GeminiSummary
    ): JobSummaryResult {

        val snapshot = jobSnapshotRepository.save(
            JobSnapshot(
                brandId = brand.id,
                positionId = position.id,
                sourceType = JobSourceType.TEXT,
                rawText = rawText,
                contentHash = contentHash
            )
        )

        val savedSummary = jobSummaryRepository.save(
            JobSummary(
                jobSnapshotId = snapshot.id,

                brandId = brand.id,
                brandName = brand.name,

                positionId = position.id,
                positionName = position.name,

                careerType = summary.careerType,
                careerYears = summary.careerYears,

                summaryText = summary.summary,
                responsibilities = summary.responsibilities,
                requiredQualifications = summary.requiredQualifications,
                preferredQualifications = summary.preferredQualifications,
                techStack = summary.techStack,
                recruitmentProcess = summary.recruitmentProcess,

                modelVersion = "gemini"
            )
        )

        return JobSummaryResult.from(savedSummary)
    }

    /**
     * ===== utils =====
     */

    private fun canonicalize(text: String): String =
        text
            .lowercase()
            .replace(Regex("\\s+"), " ")
            .trim()

    private fun sha256(input: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray())
            .joinToString("") { "%02x".format(it) }
}
