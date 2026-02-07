package com.hirelog.api.relation.application.memberbrand.command

import com.hirelog.api.common.exception.EntityAlreadyExistsException
import com.hirelog.api.relation.application.memberbrand.query.MemberBrandQuery
import com.hirelog.api.relation.domain.model.MemberBrand
import com.hirelog.api.relation.domain.type.InterestType
import jakarta.persistence.EntityNotFoundException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * MemberBrand Write Service
 *
 * 책임:
 * - MemberBrand에 대한 쓰기 트랜잭션 경계 정의
 * - Command Port 호출 위임
 *
 * 주의:
 * - 최종 중복 판단은 DB unique constraint가 담당한다
 * - 동시성으로 발생하는 constraint violation은 도메인 예외로 번역한다
 */
@Service
class MemberBrandWriteService(
    private val memberBrandCommand: MemberBrandCommand,
    private val memberBrandQuery: MemberBrandQuery
) {

    /**
     * MemberBrand 생성
     */
    @Transactional
    fun register(
        memberId: Long,
        brandId: Long,
        interestType: InterestType
    ) {
        // 1. 빠른 실패 (UX / 의미용)
        require(
            memberBrandQuery.findByMemberIdAndBrandId(memberId, brandId) == null
        ) {
            "MemberBrand already exists. member=$memberId brand=$brandId"
        }

        val relation = MemberBrand.create(
            memberId = memberId,
            brandId = brandId,
            interestType = interestType
        )

        // 2. DB가 최종 중복 판단
        try {
            memberBrandCommand.save(relation)
        } catch (ex: DataIntegrityViolationException) {
            // 3. 동시성 중복 → 도메인 예외로 번역
            throw EntityAlreadyExistsException(
                "MemberBrand already exists. member=$memberId brand=$brandId",
                ex
            )
        }
    }

    /**
     * MemberBrand 수정
     *
     * - Dirty Checking 기반
     */
    @Transactional
    fun changeInterestType(
        memberId: Long,
        brandId: Long,
        interestType: InterestType
    ) {
        val relation = memberBrandQuery
            .findByMemberIdAndBrandId(memberId, brandId)
            ?: throw EntityNotFoundException(
                "MemberBrand not found. member=$memberId brand=$brandId"
            )

        relation.changeInterestType(interestType)
    }

    /**
     * MemberBrand 삭제
     */
    @Transactional
    fun unregister(
        memberId: Long,
        brandId: Long
    ) {
        memberBrandQuery
            .findByMemberIdAndBrandId(memberId, brandId)
            ?.let { memberBrandCommand.delete(it) }
    }
}
