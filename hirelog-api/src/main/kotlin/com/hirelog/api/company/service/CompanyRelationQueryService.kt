package com.hirelog.api.company.service

import com.hirelog.api.company.domain.CompanyRelation
import com.hirelog.api.company.repository.CompanyRelationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CompanyRelationQueryService(
    private val companyRelationRepository: CompanyRelationRepository
) {

    /**
     * 특정 회사의 하위 회사 목록 조회
     *
     * 역할:
     * - 자회사 / 관계사 트리 구성에 사용
     */
    @Transactional(readOnly = true)
    fun findChildren(parentCompanyId: Long): List<CompanyRelation> =
        companyRelationRepository.findAllByParentCompanyId(parentCompanyId)

    /**
     * 특정 회사의 상위 회사 목록 조회
     *
     * 역할:
     * - 모회사 / 상위 그룹 탐색
     */
    @Transactional(readOnly = true)
    fun findParents(childCompanyId: Long): List<CompanyRelation> =
        companyRelationRepository.findAllByChildCompanyId(childCompanyId)

    /**
     * 두 회사 간 관계 단건 조회
     *
     * 역할:
     * - 관계 존재 여부 확인
     */
    @Transactional(readOnly = true)
    fun findRelation(
        parentCompanyId: Long,
        childCompanyId: Long
    ): CompanyRelation? =
        companyRelationRepository.findByParentCompanyIdAndChildCompanyId(
            parentCompanyId,
            childCompanyId
        )
}
