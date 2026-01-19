package com.hirelog.api.relation.service

import com.hirelog.api.relation.domain.MemberCompany
import com.hirelog.api.relation.repository.MemberCompanyRepository
import org.springframework.stereotype.Service

@Service
class MemberCompanyQueryService(
    private val memberCompanyRepository: MemberCompanyRepository
) {

    /**
     * 사용자의 관심 회사 목록 조회
     */
    fun findByMember(memberId: Long): List<MemberCompany> =
        memberCompanyRepository.findAllByMemberId(memberId)

    /**
     * 특정 회사를 관심 등록한 사용자 조회
     */
    fun findByCompany(companyId: Long): List<MemberCompany> =
        memberCompanyRepository.findAllByCompanyId(companyId)
}
