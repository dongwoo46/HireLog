package com.hirelog.api.relation.repository

import com.hirelog.api.relation.domain.MemberCompany
import org.springframework.data.jpa.repository.JpaRepository

interface MemberCompanyRepository : JpaRepository<MemberCompany, Long> {

    /**
     * 특정 사용자의 관심 회사 목록 조회
     */
    fun findAllByMemberId(memberId: Long): List<MemberCompany>

    /**
     * 특정 회사를 관심 등록한 사용자 조회
     */
    fun findAllByCompanyId(companyId: Long): List<MemberCompany>

    /**
     * 중복 관심 등록 방지
     */
    fun existsByMemberIdAndCompanyId(
        memberId: Long,
        companyId: Long
    ): Boolean

    /**
     * 단건 조회
     */
    fun findByMemberIdAndCompanyId(
        memberId: Long,
        companyId: Long
    ): MemberCompany?
}
