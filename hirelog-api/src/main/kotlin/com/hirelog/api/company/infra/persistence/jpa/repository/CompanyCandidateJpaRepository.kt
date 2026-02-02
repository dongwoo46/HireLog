package com.hirelog.api.company.infra.persistence.jpa.repository

import com.hirelog.api.company.domain.CompanyCandidate
import com.hirelog.api.company.domain.CompanyCandidateStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CompanyCandidateJpaRepository : JpaRepository<CompanyCandidate, Long> {

    /**
     * 특정 Brand에 귀속된 CompanyCandidate 목록 조회
     *
     * 용도:
     * - 관리자 검토 UI
     */
    fun findAllByBrandId(brandId: Long): List<CompanyCandidate>

    /**
     * 상태 기준 후보 조회
     *
     * 용도:
     * - 승인 대기(PENDING) 큐
     */
    fun findAllByStatus(status: CompanyCandidateStatus): List<CompanyCandidate>

    /**
     * Brand + 정규화 법인명 기준 중복 후보 조회
     *
     * 용도:
     * - 동일 JD / 동일 Brand에서
     *   CompanyCandidate 중복 생성 방지
     */
    fun findByBrandIdAndNormalizedName(
        brandId: Long,
        normalizedName: String
    ): CompanyCandidate?
}
