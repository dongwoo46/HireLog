package com.hirelog.api.relation.application.company.facade

import com.hirelog.api.relation.application.company.command.MemberCompanyWriteService
import com.hirelog.api.relation.domain.type.InterestType
import org.springframework.stereotype.Service

/**
 * MemberCompany Facade Service
 *
 * 책임:
 * - 회원-회사 관계 쓰기 유스케이스의 단일 진입점
 *
 * 설계 원칙:
 * - 조회(Query) 책임 없음
 * - 트랜잭션 없음
 * - ID 전달만 수행
 */
@Service
class MemberCompanyFacadeService(
    private val memberCompanyWriteService: MemberCompanyWriteService
) {

    /**
     * 회사 관심 등록
     */
    fun register(
        memberId: Long,
        companyId: Long,
        interestType: InterestType
    ) {
        memberCompanyWriteService.register(
            memberId = memberId,
            companyId = companyId,
            interestType = interestType
        )
    }

    /**
     * 관심 유형 변경
     */
    fun changeInterestType(
        memberId: Long,
        companyId: Long,
        interestType: InterestType
    ) {
        memberCompanyWriteService.changeInterestType(
            memberId = memberId,
            companyId = companyId,
            interestType = interestType
        )
    }

    /**
     * 관심 해제
     */
    fun unregister(
        memberId: Long,
        companyId: Long
    ) {
        memberCompanyWriteService.unregister(
            memberId = memberId,
            companyId = companyId
        )
    }
}
