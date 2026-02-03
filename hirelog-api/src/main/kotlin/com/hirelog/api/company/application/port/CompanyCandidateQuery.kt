package com.hirelog.api.company.application.port

import com.hirelog.api.company.application.view.CompanyCandidateView
import com.hirelog.api.company.domain.CompanyCandidateStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

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
     *
     * 용도:
     * - 상세 화면
     * - 관리자 확인
     */
    fun findViewById(id: Long): CompanyCandidateView?

    /**
     * Brand 기준 후보 조회 (페이지네이션)
     *
     * 용도:
     * - Brand 단위 후보 관리
     */
    fun findAllViewsByBrandId(
        brandId: Long,
        pageable: Pageable
    ): Page<CompanyCandidateView>

    /**
     * 상태 기준 후보 조회 (페이지네이션)
     *
     * 용도:
     * - 승인 대기 / 처리 완료 목록
     */
    fun findAllViewsByStatus(
        status: CompanyCandidateStatus,
        pageable: Pageable
    ): Page<CompanyCandidateView>

    /**
     * 중복 후보 존재 여부 확인
     *
     * 용도:
     * - 후보 생성 전 사전 검증
     */
    fun existsByBrandIdAndNormalizedName(
        brandId: Long,
        normalizedName: String
    ): Boolean
}
