package com.hirelog.api.relation.infra.persistence.jpa.repository

import com.hirelog.api.relation.domain.model.MemberCompany
import org.springframework.data.jpa.repository.JpaRepository

/**
 * MemberCompany JPA Repository
 *
 * 책임:
 * - MemberCompany 엔티티 조회/저장
 * - 회사 관심 관계에 대한 단순 DB 접근
 */
interface MemberCompanyJpaRepository : JpaRepository<MemberCompany, Long> {

    /**
     * 특정 사용자의 관심 회사 목록 조회
     */
    fun findAllByMemberId(memberId: Long): List<MemberCompany>

    /**
     * 특정 회사를 관심 등록한 사용자 목록 조회
     */
    fun findAllByCompanyId(companyId: Long): List<MemberCompany>

    /**
     * 중복 관심 등록 여부 확인
     */
    fun existsByMemberIdAndCompanyId(
        memberId: Long,
        companyId: Long
    ): Boolean

    /**
     * 단건 관계 조회
     */
    fun findByMemberIdAndCompanyId(
        memberId: Long,
        companyId: Long
    ): MemberCompany?
}
