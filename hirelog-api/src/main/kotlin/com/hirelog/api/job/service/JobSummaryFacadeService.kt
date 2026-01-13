package com.hirelog.api.job.service

import com.hirelog.api.company.domain.Position
import com.hirelog.api.company.repository.CompanyRepository
import com.hirelog.api.company.repository.PositionRepository
import com.hirelog.api.job.domain.JobSnapshot
import com.hirelog.api.job.domain.JobSourceType
import com.hirelog.api.job.domain.JobSummary
import com.hirelog.api.job.repository.JobSnapshotRepository
import com.hirelog.api.job.repository.JobSummaryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest

@Service
class JobSummaryFacadeService(
    private val companyRepository: CompanyRepository,
    private val positionRepository: PositionRepository,
    private val jobSnapshotRepository: JobSnapshotRepository,
    private val jobSummaryRepository: JobSummaryRepository,
    private val geminiService: GeminiService
) {

    /**
     * TEXT 기반 JD 요약 + 저장
     *
     * @param canonicalText FastAPI 전처리 결과 (중복 체크 기준)
     */
    @Transactional
    fun summarizeTextJDAndSave(
        companyName: String,
        positionName: String,
        rawText: String,
        canonicalText: String
    ): JobSummary {

        // 1️⃣ Company 조회
        val company = companyRepository.findByNormalizedName(normalize(companyName))
            ?: throw IllegalArgumentException("등록되지 않은 회사")

        // 2️⃣ Position 확보 (회사 종속)
        val normalizedPosition = normalize(positionName)
        val position = positionRepository
            .findByCompanyIdAndNormalizedName(company.id, normalizedPosition)
            ?: positionRepository.save(
                Position(
                    companyId = company.id,
                    name = positionName,
                    normalizedName = normalizedPosition
                )
            )

        // 3️⃣ 중복 체크용 hash (canonical 기준)
        val contentHash = sha256(
            "${company.id}|${position.id}|$canonicalText"
        )

        if (jobSnapshotRepository.existsByContentHash(contentHash)) {
            throw IllegalStateException("이미 저장된 JD")
        }

        // 4️⃣ Snapshot 저장 (RAW 보존)
        val snapshot = jobSnapshotRepository.save(
            JobSnapshot(
                companyId = company.id,
                positionId = position.id,
                sourceType = JobSourceType.TEXT,
                rawText = rawText,
                contentHash = contentHash
            )
        )

        // 5️⃣ Gemini 호출 (요약 전용)
        val summaryResult = geminiService.summaryTextJobDescription(
            companyName = company.name,
            position = position.name,
            jdText = rawText
        )

        // 6️⃣ Summary 저장 (조회 최적화용 비정규화)
        return jobSummaryRepository.save(
            JobSummary(
                jobSnapshotId = snapshot.id,

                // 🔽 비정규화 필드
                companyId = company.id,
                companyName = company.name,
                positionId = position.id,
                positionName = position.name,

                summaryText = summaryResult.summary,
                responsibilities = summaryResult.responsibilities,
                requiredQualifications = summaryResult.requiredQualifications,
                preferredQualifications = summaryResult.preferredQualifications,
                techStack = summaryResult.techStack,
                recruitmentProcess = summaryResult.recruitmentProcess,
                modelVersion = "gemini"
            )
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
