package com.hirelog.api.relation.repository

import com.hirelog.api.relation.domain.MemberJobSummary
import org.springframework.data.jpa.repository.JpaRepository

interface MemberJobSummaryRepository : JpaRepository<MemberJobSummary, Long> {

    /**
     * 사용자가 저장한 JD 목록 조회
     */
    fun findAllByMemberId(memberId: Long): List<MemberJobSummary>

    /**
     * 특정 JD를 저장한 사용자 조회
     */
    fun findAllByJobSummaryId(jobSummaryId: Long): List<MemberJobSummary>

    /**
     * 중복 저장 방지
     */
    fun existsByMemberIdAndJobSummaryId(
        memberId: Long,
        jobSummaryId: Long
    ): Boolean

    /**
     * 단건 조회
     */
    fun findByMemberIdAndJobSummaryId(
        memberId: Long,
        jobSummaryId: Long
    ): MemberJobSummary?
}
