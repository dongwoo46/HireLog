package com.hirelog.api.relation.service

import com.hirelog.api.relation.domain.InterestType
import com.hirelog.api.relation.domain.MemberCompany
import com.hirelog.api.relation.repository.MemberCompanyRepository
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service

@Service
class MemberCompanyCommandService(
    private val memberCompanyRepository: MemberCompanyRepository
) {

    /**
     * 회사 관심 등록
     */
    @Transactional
    fun register(
        memberId: Long,
        companyId: Long,
        interestType: InterestType
    ): MemberCompany {

        require(!memberCompanyRepository.existsByMemberIdAndCompanyId(memberId, companyId)) {
            "MemberCompany already exists. member=$memberId company=$companyId"
        }

        return memberCompanyRepository.save(
            MemberCompany.create(memberId, companyId, interestType)
        )
    }

    /**
     * 관심 유형 변경
     */
    @Transactional
    fun changeInterestType(
        memberId: Long,
        companyId: Long,
        interestType: InterestType
    ) {
        val relation = memberCompanyRepository
            .findByMemberIdAndCompanyId(memberId, companyId)
            ?: throw IllegalArgumentException("MemberCompany not found")

        relation.changeInterestType(interestType)
    }

    /**
     * 관심 해제
     */
    @Transactional
    fun unregister(memberId: Long, companyId: Long) {
        memberCompanyRepository
            .findByMemberIdAndCompanyId(memberId, companyId)
            ?.let { memberCompanyRepository.delete(it) }
    }
}
