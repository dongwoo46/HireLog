package com.hirelog.api.member.application.port

import com.hirelog.api.member.domain.Member

/**
 * Member Write Port
 *
 * 책임:
 * - Member Aggregate 영속화 추상화
 *
 * 설계 원칙:
 * - 유스케이스 ❌
 * - 상태 변경 ❌
 * - 저장 행위만 표현
 */
interface MemberCommand {

    fun save(member: Member): Member
}
