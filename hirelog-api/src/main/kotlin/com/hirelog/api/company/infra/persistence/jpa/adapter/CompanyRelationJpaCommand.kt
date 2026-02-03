package com.hirelog.api.company.infra.persistence.jpa.adapter

import com.hirelog.api.company.application.port.CompanyRelationCommand
import com.hirelog.api.company.domain.CompanyRelation
import com.hirelog.api.company.infra.persistence.jpa.repository.CompanyRelationJpaRepository
import org.springframework.stereotype.Component

@Component
class CompanyRelationJpaCommand(
    private val repository: CompanyRelationJpaRepository
) : CompanyRelationCommand {

    /**
     * 관계 저장
     *
     * - 신규 생성 전용
     * - Dirty Checking 전제
     */
    override fun save(relation: CompanyRelation) {
        repository.save(relation)
    }

    /**
     * 관계 삭제
     *
     * - idempotent 삭제 허용
     */
    override fun delete(relation: CompanyRelation) {
        repository.delete(relation)
    }

    /**
     * 수정/삭제 목적 단건 조회
     *
     * - 락 없음
     * - 존재 여부 판단은 Application Service 책임
     */
    override fun findById(id: Long): CompanyRelation? {
        return repository.findById(id).orElse(null)
    }
}
