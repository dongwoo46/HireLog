package com.hirelog.api.company.infra.persistence.jpa.repository

import com.hirelog.api.company.domain.CompanyCandidate
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.QueryHints
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import jakarta.persistence.QueryHint

@Repository
interface CompanyCandidateJpaRepository : JpaRepository<CompanyCandidate, Long> {

    /**
     * 수정 목적 단건 조회
     *
     * 책임:
     * - approve / reject 등 상태 변경 전용 조회
     *
     * 특징:
     * - 비관적 락(PESSIMISTIC_WRITE)
     * - 동시 승인/거절 방지
     * - 잠금 대기 시간 제한 (DB 장애 방지)
     *
     * 주의:
     * - 반드시 트랜잭션 내부에서 호출
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(
        value = [
            QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000")
        ]
    )
    @Query("select c from CompanyCandidate c where c.id = :id")
    fun findByIdForUpdate(
        @Param("id") id: Long
    ): CompanyCandidate?

    /**
     * Brand + 정규화 법인명 기준 중복 여부 확인
     *
     * 용도:
     * - 후보 생성 전 사전 검증
     *
     * 주의:
     * - 동시성 완전 보장은 DB Unique Index와 함께 사용해야 함
     */
    fun existsByBrandIdAndNormalizedName(
        brandId: Long,
        normalizedName: String
    ): Boolean

    fun findByNormalizedName(normalizedName: String): CompanyCandidate?
}
