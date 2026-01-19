package com.hirelog.api.relation.service

import com.hirelog.api.relation.domain.MemberJobSummary
import com.hirelog.api.relation.repository.MemberJobSummaryRepository
import org.springframework.stereotype.Service

@Service
class MemberJobSummaryQueryService(
    private val memberJobSummaryRepository: MemberJobSummaryRepository
) {

    /**
     * 사용자가 저장한 JD 목록 조회
     */
    fun findByMember(memberId: Long): List<MemberJobSummary> =
        memberJobSummaryRepository.findAllByMemberId(memberId)

    /**
     * 특정 JD를 저장한 사용자 조회
     */
    fun findByJobSummary(jobSummaryId: Long): List<MemberJobSummary> =
        memberJobSummaryRepository.findAllByJobSummaryId(jobSummaryId)
}
