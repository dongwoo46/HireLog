package com.hirelog.api.relation.application.company.command

import com.hirelog.api.relation.domain.model.MemberCompany
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * MemberCompany Write Service
 *
 * 책임:
 * - MemberCompany 쓰기 트랜잭션 경계 정의
 * - Command Port 호출 위임
 *
 * 주의:
 * - 중복 정책 ❌
 * - 조회 판단 ❌
 */
@Service
class MemberCompanyWriteService(
    private val memberCompanyCommand: MemberCompanyCommand
) {

    /**
     * MemberCompany 생성
     */
    @Transactional
    fun create(memberCompany: MemberCompany): MemberCompany {
        return memberCompanyCommand.save(memberCompany)
    }

    /**
     * MemberCompany 수정
     *
     * - Dirty Checking 기반
     */
    @Transactional
    fun update(memberCompany: MemberCompany) {
        memberCompanyCommand.save(memberCompany)
    }

    /**
     * MemberCompany 삭제
     */
    @Transactional
    fun delete(memberCompany: MemberCompany) {
        memberCompanyCommand.delete(memberCompany)
    }
}
