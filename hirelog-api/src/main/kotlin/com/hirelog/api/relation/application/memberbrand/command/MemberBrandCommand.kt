package com.hirelog.api.relation.application.memberbrand.command

import com.hirelog.api.relation.domain.model.MemberBrand

/**
 * MemberBrand Command Port
 *
 * 책임:
 * - MemberBrand에 대한 쓰기 행위 정의
 * - 영속성 구현(JPA 등)과 분리
 *
 * 설계 원칙:
 * - 기술적 행위(save/delete)만 노출
 * - 비즈니스 의미(create/update)는 Application 계층에서 표현
 */
interface MemberBrandCommand {

    /**
     * MemberBrand 저장
     *
     * - 신규 생성 / 수정 공통
     */
    fun save(memberBrand: MemberBrand): MemberBrand

    /**
     * MemberBrand 삭제
     */
    fun delete(memberBrand: MemberBrand)
}
