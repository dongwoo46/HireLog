package com.hirelog.api.company.infra.persistence.jpa.repository

import com.hirelog.api.company.domain.Company
import com.hirelog.api.company.domain.CompanyCandidate
import com.hirelog.api.company.domain.CompanyCandidateStatus
import jakarta.persistence.LockModeType
import jakarta.persistence.QueryHint
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.QueryHints
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface CompanyCandidateJpaRepository : JpaRepository<CompanyCandidate, Long> {

    /**
     * 상태 변경 목적 단건 조회
     *
     * - approve / reject 전용
     * - 비관적 락으로 동시 수정 방지
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(
        QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000")
    )
    @Query("select c from CompanyCandidate c where c.id = :id")
    fun findByIdForUpdate(
        @Param("id") id: Long
    ): CompanyCandidate?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(
        QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000")
    )
    @Query(
        """
        select c
        from CompanyCandidate c
        where c.status = :status
        order by c.id asc
        """
    )
    fun findApprovedForUpdate(
        @Param("status") status: CompanyCandidateStatus,
        pageable: Pageable
    ): List<CompanyCandidate>

    fun existsByBrandIdAndNormalizedName(
        brandId: Long,
        normalizedName: String
    ): Boolean


}
