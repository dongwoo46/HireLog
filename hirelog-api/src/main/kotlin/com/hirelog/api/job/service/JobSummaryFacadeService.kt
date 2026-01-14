package com.hirelog.api.job.service

import com.hirelog.api.company.domain.Brand
import com.hirelog.api.company.domain.BrandSource
import com.hirelog.api.company.domain.BrandVerificationStatus
import com.hirelog.api.company.domain.Position
import com.hirelog.api.company.repository.BrandRepository
import com.hirelog.api.company.repository.PositionRepository
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

    @Transactional
    fun summarizeTextJDAndSave(
        brandName: String,
        positionName: String,
        rawText: String
    ): JobSummaryResult {

        // 1️⃣ Brand (JD 기준 주체)
        val brand = brandService.getOrCreate(brandName)

        // 2️⃣ Position (Brand 종속)
        val position = positionService.getOrCreate(
            brandId = brand.id,
            positionName = positionName
        )

        // 3️⃣ canonicalText (임시 구현)
        // TODO [TEXT_PREPROCESSING]
        // - Python(FastAPI)에서 canonicalText 생성
        // - 규칙:
        //   1) Unicode NFKC
        //   2) lowercase
        //   3) 연속 공백/개행 정리
        //   4) 특수문자 최소화
        val canonicalText = rawText
            .lowercase()
            .replace(Regex("\\s+"), " ")
            .trim()

        // 4️⃣ 중복 체크
        val contentHash = sha256("${brand.id}|${position.id}|$canonicalText")

        if (jobSnapshotRepository.existsByContentHash(contentHash)) {
            throw IllegalStateException("이미 저장된 JD")
        }

        // 5️⃣ Snapshot (RAW 보존)
        val snapshot = jobSnapshotRepository.save(
            JobSnapshot(
                brandId = brand.id,
                positionId = position.id,
                sourceType = JobSourceType.TEXT,
                rawText = rawText,
                contentHash = contentHash
            )
        )

        // 6️⃣ Gemini 요약
        val summary = geminiService.summaryTextJobDescription(
            brandName = brand.name,
            position = position.name,
            jdText = rawText
        )

        // 7️⃣ Summary 저장 (🔥 경력 필드 포함)
        val savedSummary = jobSummaryRepository.save(
            JobSummary(
                jobSnapshotId = snapshot.id,

                brandId = brand.id,
                brandName = brand.name,

                positionId = position.id,
                positionName = position.name,

                // 🔥 경력 정보
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

        // 8️⃣ Entity → DTO 변환
        return JobSummaryResult(
            brandName = savedSummary.brandName,
            position = savedSummary.positionName,

            careerType = savedSummary.careerType,
            careerYears = savedSummary.careerYears,

            summary = savedSummary.summaryText,
            responsibilities = savedSummary.responsibilities,
            requiredQualifications = savedSummary.requiredQualifications,
            preferredQualifications = savedSummary.preferredQualifications,
            techStack = savedSummary.techStack,
            recruitmentProcess = savedSummary.recruitmentProcess
        )
    }


    private fun normalize(value: String): String =
        value.lowercase()
            .replace(Regex("[^a-z0-9가-힣]"), "")
            .trim()

    private fun sha256(input: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray())
            .joinToString("") { "%02x".format(it) }
}
