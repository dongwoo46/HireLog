package com.hirelog.api.company.infra.persistence.jpa.repository

import com.hirelog.api.company.domain.CompanyRelation
import org.springframework.data.jpa.repository.JpaRepository

interface CompanyRelationJpaRepository : JpaRepository<CompanyRelation, Long> {

    /**
     * 두 회사 간 관계 존재 여부 확인
     *
     * 역할:
     * - 중복 관계 생성 방지
     */
    fun existsByParentCompanyIdAndChildCompanyId(
        parentCompanyId: Long,
        childCompanyId: Long
    ): Boolean

    /**
     * 두 회사 간 관계 단건 조회
     *
     * 역할:
     * - 관계 존재 여부 확인
     * - 상세 조회
     */
    fun findByParentCompanyIdAndChildCompanyId(
        parentCompanyId: Long,
        childCompanyId: Long
    ): CompanyRelation?

    /**
     * 특정 회사의 하위 회사(자회사/관계사) 조회
     *
     * 역할:
     * - 회사 트리 구성
     */
    fun findAllByParentCompanyId(
        parentCompanyId: Long
    ): List<CompanyRelation>

    /**
     * 특정 회사의 상위 회사(모회사) 조회
     *
     * 역할:
     * - 상위 그룹 탐색
     */
    fun findAllByChildCompanyId(
        childCompanyId: Long
    ): List<CompanyRelation>
}
