package com.hirelog.api.position.service

import com.hirelog.api.relation.domain.MemberBrand
import com.hirelog.api.relation.repository.MemberBrandRepository
import org.springframework.stereotype.Service

@Service
class MemberBrandQueryService(
    private val memberBrandRepository: MemberBrandRepository
) {

    /**
     * 사용자의 관심 브랜드 목록 조회
     */
    fun findByMember(memberId: Long): List<MemberBrand> =
        memberBrandRepository.findAllByMemberId(memberId)

    /**
     * 특정 브랜드를 관심 등록한 사용자 조회
     */
    fun findByBrand(brandId: Long): List<MemberBrand> =
        memberBrandRepository.findAllByBrandId(brandId)
}
