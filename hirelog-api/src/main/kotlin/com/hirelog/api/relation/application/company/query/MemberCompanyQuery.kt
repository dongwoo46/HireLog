package com.hirelog.api.relation.application.company.query

import com.hirelog.api.relation.domain.model.MemberCompany

/**
 * MemberCompany Query Port
 *
 * 책임:
 * - MemberCompany 조회 유스케이스 정의
 * - 조회 전용 (Side Effect 없음)
 */
interface MemberCompanyQuery {

    /**
     * 사용자의 관심 회사 목록 조회
     */
    fun findAllByMemberId(memberId: Long): List<MemberCompany>

    /**
     * 특정 회사를 관심 등록한 사용자 목록 조회
     */
    fun findAllByCompanyId(companyId: Long): List<MemberCompany>

    /**
     * 단건 관계 조회
     *
     * - Facade에서 상태 변경 전 검증용
     */
    fun findByMemberIdAndCompanyId(
        memberId: Long,
        companyId: Long
    ): MemberCompany?
}
