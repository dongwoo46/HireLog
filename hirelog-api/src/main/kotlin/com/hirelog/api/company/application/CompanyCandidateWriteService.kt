package com.hirelog.api.company.application

import com.hirelog.api.company.application.port.CompanyCandidateCommand
import com.hirelog.api.company.application.port.CompanyCandidateQuery
import com.hirelog.api.company.domain.CompanyCandidate
import com.hirelog.api.company.domain.CompanyCandidateSource
import com.hirelog.api.company.domain.CompanyCandidateStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * CompanyCandidateWriteService
 *
 * 책임:
 * - CompanyCandidate 생성
 * - 상태 전이 (approve / reject)
 *
 * 트랜잭션 정책:
 * - Application Service에서만 트랜잭션 관리
 */
@Service
class CompanyCandidateWriteService(
    private val companyCandidateCommand: CompanyCandidateCommand,
    private val companyCandidateQuery: CompanyCandidateQuery
) {

    /**
     * CompanyCandidate 생성
     *
     * 정책:
     * - 동일 Brand + normalizedName 후보가 이미 존재하면 생성하지 않음
     * - 항상 PENDING 상태로 시작
     */
    @Transactional
    fun createCandidate(
        jdSummaryId: Long,
        brandId: Long,
        candidateName: String,
        source: CompanyCandidateSource,
        confidenceScore: Double,
    ): CompanyCandidate {

        val normalizedName = normalize(candidateName)

        val existing = companyCandidateQuery.findByBrandIdAndNormalizedName(
            brandId = brandId,
            normalizedName = normalizedName
        )

        if (existing != null) {
            return existing
        }

        val candidate = CompanyCandidate.create(
            jdSummaryId = jdSummaryId,
            brandId = brandId,
            candidateName = candidateName,
            source = source,
            confidenceScore = confidenceScore,
        )

        companyCandidateCommand.save(candidate)
        return candidate
    }

    /**
     * CompanyCandidate 승인
     *
     * 정책:
     * - PENDING 상태만 승인 가능
     * - 승인 자체는 상태 전이만 수행
     */
    @Transactional
    fun approve(candidateId: Long) {
        val candidate = getPendingCandidate(candidateId)
        candidate.approve()
    }

    /**
     * CompanyCandidate 거절
     *
     * 정책:
     * - 언제든 REJECTED 가능
     */
    @Transactional
    fun reject(candidateId: Long) {
        val candidate = companyCandidateQuery.findById(candidateId)
            ?: return

        candidate.reject()
    }

    /**
     * 내부 유틸: 승인 가능한 후보 조회
     */
    private fun getPendingCandidate(candidateId: Long): CompanyCandidate {
        val candidate = companyCandidateQuery.findById(candidateId)
            ?: error("CompanyCandidate not found: id=$candidateId")

        require(candidate.status == CompanyCandidateStatus.PENDING) {
            "Only PENDING CompanyCandidate can be approved. id=$candidateId"
        }

        return candidate
    }

    /**
     * 법인명 정규화 규칙
     *
     * 주의:
     * - CompanyCandidate.normalize와 반드시 동일해야 함
     * - 추후 공통 유틸로 분리 가능
     */
    private fun normalize(value: String): String =
        value
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
}
