package com.hirelog.api.relation.application.jobsummary.command

import com.hirelog.api.relation.domain.model.MemberJobSummary
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * MemberJobSummary Write Service
 *
 * 책임:
 * - MemberJobSummary 쓰기 트랜잭션 경계 정의
 * - Command Port 호출 위임
 *
 * 주의:
 * - 중복 정책 ❌
 * - 조회 판단 ❌
 */
@Service
class MemberJobSummaryWriteService(
    private val memberJobSummaryCommand: MemberJobSummaryCommand
) {

    /**
     * MemberJobSummary 생성
     */
    @Transactional
    fun create(memberJobSummary: MemberJobSummary): MemberJobSummary {
        return memberJobSummaryCommand.save(memberJobSummary)
    }

    /**
     * MemberJobSummary 수정
     *
     * - Dirty Checking 기반
     */
    @Transactional
    fun update(memberJobSummary: MemberJobSummary) {
        memberJobSummaryCommand.save(memberJobSummary)
    }

    /**
     * MemberJobSummary 삭제
     */
    @Transactional
    fun delete(memberJobSummary: MemberJobSummary) {
        memberJobSummaryCommand.delete(memberJobSummary)
    }
}
