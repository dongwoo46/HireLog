package com.hirelog.api.company.application

import com.hirelog.api.common.exception.EntityAlreadyExistsException
import com.hirelog.api.company.application.port.CompanyRelationCommand
import com.hirelog.api.company.application.port.CompanyRelationQuery
import com.hirelog.api.company.domain.CompanyRelation
import com.hirelog.api.company.domain.CompanyRelationType
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * CompanyRelationWriteService
 *
 * 책임:
 * - CompanyRelation 쓰기 유스케이스 전담
 * - 트랜잭션 경계 정의
 * - 관계 생성/삭제 정책 보장
 */
@Service
class CompanyRelationWriteService(
    private val relationCommand: CompanyRelationCommand,
    private val relationQuery: CompanyRelationQuery
) {

    /**
     * 회사 관계 생성
     *
     * 정책:
     * - (parent, child) 관계는 유일해야 함
     * - 중복 생성 시 EntityAlreadyExistsException 발생
     *
     * 전략:
     * - 사전 조회 ❌
     * - DB 유니크 제약을 최종 진실로 신뢰
     */
    @Transactional
    fun create(
        parentCompanyId: Long,
        childCompanyId: Long,
        relationType: CompanyRelationType
    ): CompanyRelation {

        val relation = CompanyRelation.create(
            parentCompanyId = parentCompanyId,
            childCompanyId = childCompanyId,
            relationType = relationType
        )

        return try {
            relationCommand.save(relation)
            relation
        } catch (ex: DataIntegrityViolationException) {
            throw EntityAlreadyExistsException(
                entityName = "CompanyRelation",
                identifier = "parent=$parentCompanyId, child=$childCompanyId",
                cause = ex
            )
        }
    }

    /**
     * 회사 관계 삭제
     *
     * 정책:
     * - 존재할 경우에만 삭제
     * - 없으면 no-op
     *
     * 전략:
     * - Query(View)로 존재 확인
     * - Command로 엔티티 삭제
     */
    @Transactional
    fun delete(
        parentCompanyId: Long,
        childCompanyId: Long
    ) {
        val view = relationQuery.findView(parentCompanyId, childCompanyId)
            ?: return

        val relation = relationCommand.findById(view.id)
            ?: return

        relationCommand.delete(relation)
    }
}
