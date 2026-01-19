package com.hirelog.api.relation.repository

import com.hirelog.api.relation.domain.MemberBrand
import org.springframework.data.jpa.repository.JpaRepository

interface MemberBrandRepository : JpaRepository<MemberBrand, Long> {

    /**
     * 특정 사용자의 관심 브랜드 목록 조회
     */
    fun findAllByMemberId(memberId: Long): List<MemberBrand>

    /**
     * 특정 브랜드를 관심 등록한 사용자 조회
     */
    fun findAllByBrandId(brandId: Long): List<MemberBrand>

    /**
     * 중복 관심 등록 방지
     */
    fun existsByMemberIdAndBrandId(
        memberId: Long,
        brandId: Long
    ): Boolean

    /**
     * 단건 조회 (관계 존재 여부 확인)
     */
    fun findByMemberIdAndBrandId(
        memberId: Long,
        brandId: Long
    ): MemberBrand?
}
