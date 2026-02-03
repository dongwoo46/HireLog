package com.hirelog.api.company.application.port

import com.hirelog.api.company.domain.CompanyRelation

/**
 * CompanyRelationCommand
 *
 * 책임:
 * - CompanyRelation 쓰기 전용
 */
interface CompanyRelationCommand {

    fun save(relation: CompanyRelation)

    fun delete(relation: CompanyRelation)

    /**
     * 수정/삭제 목적 조회
     *
     * 특징:
     * - 비관적 락
     */
    fun findById(id: Long): CompanyRelation?
}
