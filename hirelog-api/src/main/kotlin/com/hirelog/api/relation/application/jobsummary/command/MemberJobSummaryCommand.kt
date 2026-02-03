package com.hirelog.api.relation.application.jobsummary.command

import com.hirelog.api.relation.domain.model.MemberJobSummary

/**
 * MemberJobSummary Command Port
 *
 * 책임:
 * - MemberJobSummary에 대한 쓰기 행위 정의
 * - 영속성 구현(JPA 등)과 분리
 */
interface MemberJobSummaryCommand {

    /**
     * MemberJobSummary 저장
     *
     * - 신규 생성 / 수정 공통
     */
    fun save(memberJobSummary: MemberJobSummary): MemberJobSummary

    /**
     * MemberJobSummary 삭제
     */
    fun delete(memberJobSummary: MemberJobSummary)

    /**
     * Write 전용 엔티티 조회
     * - 상태 변경 대상 확보 목적
     */
    fun findEntityByMemberIdAndJobSummaryId(
        memberId: Long,
        jobSummaryId: Long
    ): MemberJobSummary?
}
