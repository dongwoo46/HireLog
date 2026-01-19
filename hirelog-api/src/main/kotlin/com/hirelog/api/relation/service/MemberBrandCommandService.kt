package com.hirelog.api.relation.service

import com.hirelog.api.relation.domain.InterestType
import com.hirelog.api.relation.domain.MemberBrand
import com.hirelog.api.relation.repository.MemberBrandRepository
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service

@Service
class MemberBrandCommandService(
    private val memberBrandRepository: MemberBrandRepository
) {

    /**
     * 브랜드 관심 등록
     *
     * 역할:
     * - 사용자가 브랜드를 즐겨찾기 / 관심 대상으로 등록
     */
    @Transactional
    fun register(
        memberId: Long,
        brandId: Long,
        interestType: InterestType
    ): MemberBrand {

        require(!memberBrandRepository.existsByMemberIdAndBrandId(memberId, brandId)) {
            "MemberBrand already exists. member=$memberId brand=$brandId"
        }

        return memberBrandRepository.save(
            MemberBrand.create(memberId, brandId, interestType)
        )
    }

    /**
     * 관심 유형 변경
     */
    @Transactional
    fun changeInterestType(
        memberId: Long,
        brandId: Long,
        interestType: InterestType
    ) {
        val relation = memberBrandRepository
            .findByMemberIdAndBrandId(memberId, brandId)
            ?: throw IllegalArgumentException("MemberBrand not found")

        relation.changeInterestType(interestType)
    }

    /**
     * 관심 해제
     */
    @Transactional
    fun unregister(memberId: Long, brandId: Long) {
        memberBrandRepository
            .findByMemberIdAndBrandId(memberId, brandId)
            ?.let { memberBrandRepository.delete(it) }
    }
}
