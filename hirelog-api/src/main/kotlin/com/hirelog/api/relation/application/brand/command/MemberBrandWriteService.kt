package com.hirelog.api.relation.application.brand.command

import com.hirelog.api.relation.domain.model.MemberBrand
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * MemberBrand Write Service
 *
 * 책임:
 * - MemberBrand에 대한 쓰기 트랜잭션 경계 정의
 * - Command Port 호출 위임
 *
 * 주의:
 * - 중복 정책 ❌
 * - 조회 판단 ❌
 * - 비즈니스 규칙 ❌
 */
@Service
class MemberBrandWriteService(
    private val memberBrandCommand: MemberBrandCommand
) {

    /**
     * MemberBrand 생성
     */
    @Transactional
    fun create(memberBrand: MemberBrand): MemberBrand {
        return memberBrandCommand.save(memberBrand)
    }

    /**
     * MemberBrand 수정
     *
     * - Dirty Checking 기반
     */
    @Transactional
    fun update(memberBrand: MemberBrand) {
        memberBrandCommand.save(memberBrand)
    }

    /**
     * MemberBrand 삭제
     */
    @Transactional
    fun delete(memberBrand: MemberBrand) {
        memberBrandCommand.delete(memberBrand)
    }
}
