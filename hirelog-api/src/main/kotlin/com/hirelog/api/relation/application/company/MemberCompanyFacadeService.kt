package com.hirelog.api.relation.application.company.facade

import com.hirelog.api.relation.application.company.command.MemberCompanyWriteService
import com.hirelog.api.relation.application.company.query.MemberCompanyQuery
import com.hirelog.api.relation.domain.model.MemberCompany
import com.hirelog.api.relation.domain.type.InterestType
import org.springframework.stereotype.Service

/**
 * MemberCompany Facade Service
 *
 * 책임:
 * - 회원-회사 관계 쓰기 유스케이스 오케스트레이션
 * - 중복 정책 및 상태 변경 정책 결정
 *
 * 설계 원칙:
 * - 트랜잭션 ❌ (WriteService에서만 처리)
 * - 조회 API 제공 ❌
 */
@Service
class MemberCompanyFacadeService(
    private val memberCompanyQuery: MemberCompanyQuery,
    private val memberCompanyWriteService: MemberCompanyWriteService
) {

    /**
     * 회사 관심 등록
     *
     * 정책:
     * - 동일 회원-회사 관계 중복 등록 불가
     */
    fun register(
        memberId: Long,
        companyId: Long,
        interestType: InterestType
    ): MemberCompany {

        require(
            memberCompanyQuery.findByMemberIdAndCompanyId(memberId, companyId) == null
        ) {
            "MemberCompany already exists. member=$memberId company=$companyId"
        }

        val relation = MemberCompany.create(
            memberId = memberId,
            companyId = companyId,
            interestType = interestType
        )

        return memberCompanyWriteService.create(relation)
    }

    /**
     * 관심 유형 변경
     *
     * 정책:
     * - 관계가 반드시 존재해야 함
     */
    fun changeInterestType(
        memberId: Long,
        companyId: Long,
        interestType: InterestType
    ) {
        val relation = memberCompanyQuery
            .findByMemberIdAndCompanyId(memberId, companyId)
            ?: throw IllegalArgumentException(
                "MemberCompany not found. member=$memberId company=$companyId"
            )

        relation.changeInterestType(interestType)

        memberCompanyWriteService.update(relation)
    }

    /**
     * 관심 해제
     *
     * 정책:
     * - 관계가 존재할 경우에만 삭제
     */
    fun unregister(
        memberId: Long,
        companyId: Long
    ) {
        memberCompanyQuery
            .findByMemberIdAndCompanyId(memberId, companyId)
            ?.let { memberCompanyWriteService.delete(it) }
    }
}
