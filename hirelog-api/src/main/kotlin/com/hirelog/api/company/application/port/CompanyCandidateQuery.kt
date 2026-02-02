package com.hirelog.api.company.application.port

import com.hirelog.api.company.domain.CompanyCandidate
import com.hirelog.api.company.domain.CompanyCandidateStatus

/**
 * CompanyCandidateQuery
 *
 * 책임:
 * - CompanyCandidate 조회 전용 포트
 * - 읽기 모델 관점
 */
interface CompanyCandidateQuery {

    /**
     * 단건 조회
     */
    fun findById(id: Long): CompanyCandidate?

    /**
     * Brand 기준 후보 조회
     */
    fun findAllByBrandId(brandId: Long): List<CompanyCandidate>

    /**
     * 상태 기준 후보 조회
     */
    fun findAllByStatus(status: CompanyCandidateStatus): List<CompanyCandidate>

    /**
     * Brand + 정규화 법인명 기준 조회
     *
     * 용도:
     * - 중복 후보 방지
     */
    fun findByBrandIdAndNormalizedName(
        brandId: Long,
        normalizedName: String
    ): CompanyCandidate?
}
