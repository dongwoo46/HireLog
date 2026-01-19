package com.hirelog.api.relation.application.company.command

import com.hirelog.api.relation.domain.model.MemberCompany

/**
 * MemberCompany Command Port
 *
 * 책임:
 * - MemberCompany 쓰기 행위 정의
 * - 영속성 구현(JPA 등)과 분리
 */
interface MemberCompanyCommand {

    /**
     * MemberCompany 저장
     *
     * - 신규 생성 / 수정 공통
     */
    fun save(memberCompany: MemberCompany): MemberCompany

    /**
     * MemberCompany 삭제
     */
    fun delete(memberCompany: MemberCompany)
}
