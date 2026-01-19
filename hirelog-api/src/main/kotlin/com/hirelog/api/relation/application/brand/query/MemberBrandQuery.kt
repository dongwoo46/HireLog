package com.hirelog.api.relation.application.brand.query

import com.hirelog.api.relation.domain.model.MemberBrand

/**
 * MemberBrand Query Port
 *
 * 책임:
 * - MemberBrand 조회 유스케이스 정의
 * - 조회 전용 (Side Effect 없음)
 */
interface MemberBrandQuery {

    /**
     * 사용자의 관심 브랜드 목록 조회
     */
    fun findAllByMemberId(memberId: Long): List<MemberBrand>

    /**
     * 특정 브랜드를 관심 등록한 사용자 목록 조회
     *
     * (관리/통계용 조회)
     */
    fun findAllByBrandId(brandId: Long): List<MemberBrand>

    /**
     * 단건 관계 조회
     *
     * - Facade에서 상태 변경 전 검증용
     * - 존재 여부 판단은 호출자가 수행
     */
    fun findByMemberIdAndBrandId(
        memberId: Long,
        brandId: Long
    ): MemberBrand?
}
