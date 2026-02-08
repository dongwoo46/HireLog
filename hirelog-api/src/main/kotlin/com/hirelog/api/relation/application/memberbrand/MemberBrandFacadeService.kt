package com.hirelog.api.relation.application.memberbrand.facade

import com.hirelog.api.relation.application.memberbrand.command.MemberBrandWriteService
import com.hirelog.api.relation.domain.type.InterestType
import org.springframework.stereotype.Service

/**
 * MemberBrand Facade Service
 *
 * 책임:
 * - 회원-브랜드 관계 쓰기 유스케이스 오케스트레이션
 * - 중복 정책 및 상태 변경 정책 결정
 *
 * 설계 원칙:
 * - 트랜잭션 ❌ (WriteService에서만 처리)
 * - 조회 API 제공 ❌
 */
@Service
class MemberBrandFacadeService(
    private val memberBrandWriteService: MemberBrandWriteService
) {

    /**
     * 브랜드 관심 등록
     *
     * 정책:
     * - 동일 회원-브랜드 관계 중복 등록 불가
     */
    fun register(
        memberId: Long,
        brandId: Long,
        interestType: InterestType
    ) {
        memberBrandWriteService.register(
            memberId = memberId,
            brandId = brandId,
            interestType = interestType
        )
    }

    /**
     * 관심 유형 변경
     *
     * 정책:
     * - 관계가 반드시 존재해야 함
     */
    fun changeInterestType(
        memberId: Long,
        brandId: Long,
        interestType: InterestType
    ) {
        memberBrandWriteService.changeInterestType(
            memberId = memberId,
            brandId = brandId,
            interestType = interestType
        )
    }

    /**
     * 관심 해제
     *
     * 정책:
     * - 관계가 존재할 경우에만 삭제
     */
    fun unregister(
        memberId: Long,
        brandId: Long
    ) {
        memberBrandWriteService.unregister(
            memberId = memberId,
            brandId = brandId
        )
    }
}
