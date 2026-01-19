package com.hirelog.api.relation.infra.persistence.jpa.repository

import com.hirelog.api.relation.domain.model.MemberJobSummary
import org.springframework.data.jpa.repository.JpaRepository

/**
 * MemberJobSummary JPA Repository
 *
 * 책임:
 * - 사용자가 저장한 JD 요약 관계 조회
 * - 단순 영속성 접근만 담당
 */
interface MemberJobSummaryJpaRepository : JpaRepository<MemberJobSummary, Long> {

    /**
     * 사용자가 저장한 JD 요약 목록 조회
     */
    fun findAllByMemberId(memberId: Long): List<MemberJobSummary>

    /**
     * 특정 JD 요약을 저장한 사용자 목록 조회
     */
    fun findAllByJobSummaryId(jobSummaryId: Long): List<MemberJobSummary>

    /**
     * 중복 저장 여부 확인
     */
    fun existsByMemberIdAndJobSummaryId(
        memberId: Long,
        jobSummaryId: Long
    ): Boolean

    /**
     * 단건 관계 조회
     */
    fun findByMemberIdAndJobSummaryId(
        memberId: Long,
        jobSummaryId: Long
    ): MemberJobSummary?
}
