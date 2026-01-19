package com.hirelog.api.relation.infra.persistence.jpa.repository

import com.hirelog.api.relation.domain.model.MemberBrand
import org.springframework.data.jpa.repository.JpaRepository

/**
 * MemberBrand JPA Repository
 *
 * 책임:
 * - MemberBrand 엔티티에 대한 DB 접근
 * - 순수 조회/존재 여부 판단만 수행
 *
 * 주의:
 * - 비즈니스 정책 ❌
 * - 중복 처리 흐름 ❌
 */
interface MemberBrandJpaRepository : JpaRepository<MemberBrand, Long> {

    /**
     * 특정 사용자의 관심 브랜드 목록 조회
     */
    fun findAllByMemberId(memberId: Long): List<MemberBrand>

    /**
     * 특정 브랜드를 관심 등록한 사용자 목록 조회
     */
    fun findAllByBrandId(brandId: Long): List<MemberBrand>

    /**
     * 중복 관심 등록 여부 확인
     */
    fun existsByMemberIdAndBrandId(
        memberId: Long,
        brandId: Long
    ): Boolean

    /**
     * 단건 관계 조회
     */
    fun findByMemberIdAndBrandId(
        memberId: Long,
        brandId: Long
    ): MemberBrand?
}
