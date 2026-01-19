package com.hirelog.api.relation.application.company.command

import com.hirelog.api.common.exception.EntityAlreadyExistsException
import com.hirelog.api.relation.application.company.query.MemberCompanyQuery
import com.hirelog.api.relation.domain.model.MemberCompany
import com.hirelog.api.relation.domain.type.InterestType
import jakarta.persistence.EntityExistsException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * MemberCompany Write Service
 *
 * 책임:
 * - MemberCompany 쓰기 트랜잭션 경계 정의
 * - Command Port 호출 위임
 *
 * 주의:
 * - 중복 정책 ❌
 * - 조회 판단 ❌
 */
@Service
class MemberCompanyWriteService(
    private val memberCompanyCommand: MemberCompanyCommand,
    private val memberCompanyQuery: MemberCompanyQuery
) {

    /**
     * 회사 관심 등록
     *
     * 정책:
     * - 동일 회원-회사 관계 중복 불가
     * - DB unique constraint를 최종 판단으로 사용
     */
    @Transactional
    fun register(
        memberId: Long,
        companyId: Long,
        interestType: InterestType
    ) {
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

        try {
            memberCompanyCommand.save(relation)
        } catch (ex: DataIntegrityViolationException) {
            // 동시성 중복 → 도메인 예외로 번역
            throw EntityAlreadyExistsException(
                "MemberCompany already exists. member=$memberId company=$companyId",
                ex
            )
        }
    }

    /**
     * 관심 유형 변경
     *
     * 정책:
     * - 관계가 반드시 존재해야 함
     * - 트랜잭션 내부에서 read-modify-write 수행
     */
    @Transactional
    fun changeInterestType(
        memberId: Long,
        companyId: Long,
        interestType: InterestType
    ) {
        val relation = requireNotNull(
            memberCompanyQuery.findByMemberIdAndCompanyId(memberId, companyId)
        ) {
            "MemberCompany not found. member=$memberId company=$companyId"
        }

        relation.changeInterestType(interestType)
    }

    /**
     * 관심 해제
     *
     * 정책:
     * - 관계가 존재할 경우에만 삭제
     */
    @Transactional
    fun unregister(
        memberId: Long,
        companyId: Long
    ) {
        memberCompanyQuery
            .findByMemberIdAndCompanyId(memberId, companyId)
            ?.let { memberCompanyCommand.delete(it) }
    }
}
