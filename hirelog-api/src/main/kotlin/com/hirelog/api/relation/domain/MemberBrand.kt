package com.hirelog.api.relation.domain

import com.hirelog.api.common.jpa.BaseEntity
import jakarta.persistence.*

@Entity
@Table(
    name = "member_brand",
    indexes = [
        // 사용자가 관심 있는 브랜드 조회
        Index(
            name = "idx_member_brand_member",
            columnList = "member_id"
        ),
        // 특정 브랜드를 관심 가진 사용자 조회 (분석/알림용)
        Index(
            name = "idx_member_brand_brand",
            columnList = "brand_id"
        ),
        // 중복 즐겨찾기 방지
        Index(
            name = "ux_member_brand_member_brand",
            columnList = "member_id, brand_id",
            unique = true
        )
    ]
)
class MemberBrand(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    /**
     * 사용자 ID
     */
    @Column(name = "member_id", nullable = false)
    val memberId: Long,

    /**
     * 관심 브랜드 ID
     */
    @Column(name = "brand_id", nullable = false)
    val brandId: Long,

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
         * MemberBrand 생성
         *
         * 역할:
         * - 사용자가 브랜드를 관심 대상으로 등록
         * - 기본 관심 유형은 FAVORITE
         */
        fun create(
            memberId: Long,
            brandId: Long,
            interestType: InterestType = InterestType.FAVORITE
        ): MemberBrand {
            return MemberBrand(
                memberId = memberId,
                brandId = brandId,
                interestType = interestType
            )
        }
    }

    /**
     * 관심 유형 변경
     *
     * 역할:
     * - FAVORITE ↔ WATCH 전환
     */
    fun changeInterestType(newType: InterestType) {
        if (interestType == newType) return
        interestType = newType
    }
}
