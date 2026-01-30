package com.hirelog.api.relation.domain.model

import com.hirelog.api.common.infra.jpa.entity.BaseEntity
import com.hirelog.api.relation.domain.type.InterestType
import jakarta.persistence.*

@Entity
@Table(
    name = "member_company",
    indexes = [
        // 사용자가 관심 있는 회사 조회
        Index(
            name = "idx_member_company_member",
            columnList = "member_id"
        ),
        // 특정 회사를 관심 가진 사용자 조회
        Index(
            name = "idx_member_company_company",
            columnList = "company_id"
        ),
        // 중복 관심 방지
        Index(
            name = "ux_member_company_member_company",
            columnList = "member_id, company_id",
            unique = true
        )
    ]
)
class MemberCompany(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    /**
     * 사용자 ID
     */
    @Column(name = "member_id", nullable = false)
    val memberId: Long,

    /**
     * 관심 회사 ID (법인 단위)
     */
    @Column(name = "company_id", nullable = false)
    val companyId: Long,

    /**
     * 관심 유형
     *
     * FAVORITE : 즐겨찾기
     * WATCH    : 관심 (알림/추천 대상)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "interest_type", nullable = false, length = 20)
    var interestType: InterestType = InterestType.FAVORITE

) : BaseEntity() {

    companion object {

        /**
         * MemberCompany 생성
         *
         * 역할:
         * - 사용자가 회사를 관심 대상으로 등록
         */
        fun create(
            memberId: Long,
            companyId: Long,
            interestType: InterestType = InterestType.FAVORITE
        ): MemberCompany {
            return MemberCompany(
                memberId = memberId,
                companyId = companyId,
                interestType = interestType
            )
        }
    }

    /**
     * 관심 유형 변경
     *
     * 역할:
     * - 관심/즐겨찾기 상태 전환
     */
    fun changeInterestType(newType: InterestType) {
        if (interestType == newType) return
        interestType = newType
    }
}

