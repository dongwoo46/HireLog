package com.hirelog.api.company.infra.persistence.jpa.adapter

import com.hirelog.api.company.application.query.CompanyRelationQuery
import com.hirelog.api.company.domain.CompanyRelation
import com.hirelog.api.company.infra.persistence.jpa.repository.CompanyRelationJpaRepository
import org.springframework.stereotype.Component

/**
 * CompanyRelation JPA Query Adapter
 *
 * 역할:
 * - CompanyRelationQuery 포트를 JPA 기반으로 구현
 * - 조회 로직만 담당
 *
 * 설계 원칙:
 * - 예외를 던지지 않는다
 * - 비즈니스 판단은 상위 계층(WriteService / Facade) 책임
 */
@Component
class CompanyRelationJpaQuery(
    private val relationRepository: CompanyRelationJpaRepository
) : CompanyRelationQuery {

    /**
     * 부모 회사 기준 하위 관계 조회
     */
    override fun findByParentCompanyId(
        parentCompanyId: Long
    ): List<CompanyRelation> =
        relationRepository.findAllByParentCompanyId(parentCompanyId)

    /**
     * 자회사 기준 상위 관계 조회
     */
    override fun findByChildCompanyId(
        childCompanyId: Long
    ): List<CompanyRelation> =
        relationRepository.findAllByChildCompanyId(childCompanyId)

    /**
     * 부모-자식 관계 단건 조회
     */
    override fun findRelation(
        parentCompanyId: Long,
        childCompanyId: Long
    ): CompanyRelation? =
        relationRepository.findByParentCompanyIdAndChildCompanyId(
            parentCompanyId,
            childCompanyId
        )
}
