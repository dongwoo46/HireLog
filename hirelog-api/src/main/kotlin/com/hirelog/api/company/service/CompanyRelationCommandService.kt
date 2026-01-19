package com.hirelog.api.company.service

import com.hirelog.api.company.domain.CompanyRelation
import com.hirelog.api.company.domain.CompanyRelationType
import com.hirelog.api.company.repository.CompanyRelationRepository
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service

@Service
class CompanyRelationCommandService(
    private val companyRelationRepository: CompanyRelationRepository
) {

    /**
     * 회사 관계 생성
     *
     * 역할:
     * - 두 회사 간의 관계를 생성한다
     *
     * 정책:
     * - (parentCompanyId, childCompanyId) 조합은 유니크
     * - 자기 자신과의 관계는 도메인에서 차단
     */
    @Transactional
    fun create(
        parentCompanyId: Long,
        childCompanyId: Long,
        relationType: CompanyRelationType
    ): CompanyRelation {

        // 존재하는지 검증
        require(
            !companyRelationRepository.existsByParentCompanyIdAndChildCompanyId(
                parentCompanyId,
                childCompanyId
            )
        ) {
            "CompanyRelation already exists. parent=$parentCompanyId, child=$childCompanyId"
        }

        val relation = CompanyRelation.create(
            parentCompanyId = parentCompanyId,
            childCompanyId = childCompanyId,
            relationType = relationType
        )

        return companyRelationRepository.save(relation)
    }

    /**
     * 회사 관계 삭제
     *
     * 역할:
     * - 잘못 등록된 관계를 제거한다
     *
     * 특징:
     * - 히스토리 보존이 필요 없다고 가정하고 물리 삭제
     */
    @Transactional
    fun delete(relationId: Long) {
        companyRelationRepository.deleteById(relationId)
    }
}
