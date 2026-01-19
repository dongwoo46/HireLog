package com.hirelog.api.relation.service

import com.hirelog.api.relation.domain.MemberJobSummary
import com.hirelog.api.relation.domain.MemberJobSummarySaveType
import com.hirelog.api.relation.repository.MemberJobSummaryRepository
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service

@Service
class MemberJobSummaryCommandService(
    private val memberJobSummaryRepository: MemberJobSummaryRepository
) {

    /**
     * JD 저장
     */
    @Transactional
    fun save(
        memberId: Long,
        jobSummaryId: Long,
        saveType: MemberJobSummarySaveType,
        memo: String?
    ): MemberJobSummary {

        require(
            !memberJobSummaryRepository.existsByMemberIdAndJobSummaryId(
                memberId,
                jobSummaryId
            )
        ) {
            "MemberJobSummary already exists"
        }

        return memberJobSummaryRepository.save(
            MemberJobSummary.create(
                memberId,
                jobSummaryId,
                saveType,
                memo
            )
        )
    }

    /**
     * 저장 목적 변경
     */
    @Transactional
    fun changeSaveType(
        memberId: Long,
        jobSummaryId: Long,
        saveType: MemberJobSummarySaveType
    ) {
        val relation = memberJobSummaryRepository
            .findByMemberIdAndJobSummaryId(memberId, jobSummaryId)
            ?: throw IllegalArgumentException("MemberJobSummary not found")

        relation.changeSaveType(saveType)
    }

    /**
     * 메모 수정
     */
    @Transactional
    fun updateMemo(
        memberId: Long,
        jobSummaryId: Long,
        memo: String?
    ) {
        val relation = memberJobSummaryRepository
            .findByMemberIdAndJobSummaryId(memberId, jobSummaryId)
            ?: throw IllegalArgumentException("MemberJobSummary not found")

        relation.updateMemo(memo)
    }

    /**
     * 저장 취소
     */
    @Transactional
    fun delete(memberId: Long, jobSummaryId: Long) {
        memberJobSummaryRepository
            .findByMemberIdAndJobSummaryId(memberId, jobSummaryId)
            ?.let { memberJobSummaryRepository.delete(it) }
    }
}
