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
 * CompanyRelation Write Service
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
     * - 중복 생성 시 EntityExistsException 발생
     */
    @Transactional
    fun create(
        parentCompanyId: Long,
        childCompanyId: Long,
        relationType: CompanyRelationType
    ): CompanyRelation {

        // 1️⃣ UX / 의미적 빠른 실패 (선택)
        if (relationQuery.findRelation(parentCompanyId, childCompanyId) != null) {
            throw EntityAlreadyExistsException(
                entityName = "CompanyRelation",
                identifier = "parent=$parentCompanyId, child=$childCompanyId"
            )
        }

        val relation = CompanyRelation.create(
            parentCompanyId = parentCompanyId,
            childCompanyId = childCompanyId,
            relationType = relationType
        )

        // 2️⃣ DB를 최종 진실로 신뢰
        return try {
            relationCommand.save(relation)
        } catch (ex: DataIntegrityViolationException) {
            // 3️⃣ 동시성 중복 생성 방어
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
     */
    @Transactional
    fun delete(
        parentCompanyId: Long,
        childCompanyId: Long
    ) {
        relationQuery
            .findRelation(parentCompanyId, childCompanyId)
            ?.let { relationCommand.delete(it) }
    }
}
