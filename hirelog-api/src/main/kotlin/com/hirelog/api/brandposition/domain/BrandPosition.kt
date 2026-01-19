package com.hirelog.api.brandposition.domain

import com.hirelog.api.common.jpa.BaseEntity
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "brand_position",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_brand_position_brand_id_position_id",
            columnNames = ["brand_id", "position_id"]
        )
    ],
    indexes = [
        Index(
            name = "idx_brand_position_brand_id",
            columnList = "brand_id"
        ),
        Index(
            name = "idx_brand_position_position_id",
            columnList = "position_id"
        ),
        Index(
            name = "idx_brand_position_status",
            columnList = "status"
        )
    ]
)
class BrandPosition protected constructor(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    /**
     * 브랜드 ID
     */
    @Column(name = "brand_id", nullable = false)
    val brandId: Long,

    /**
     * 시장 공통 포지션 ID
     */
    @Column(name = "position_id", nullable = false)
    val positionId: Long,

    /**
     * 브랜드 내부에서 사용하는 포지션명
     * - null이면 공통 Position.name 사용
     */
    @Column(name = "display_name", length = 200)
    val displayName: String? = null,

    /**
     * 브랜드 기준 포지션 상태
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: BrandPositionStatus,

    /**
     * 생성 출처
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 20)
    val source: BrandPositionSource,

    /**
     * 승인 시각
     */
    @Column(name = "approved_at")
    var approvedAt: LocalDateTime? = null,

    /**
     * 승인 관리자 ID
     */
    @Column(name = "approved_by")
    var approvedBy: Long? = null

) : BaseEntity() {

    /**
     * 관리자 승인 처리
     *
     * 도메인 규칙:
     * - CANDIDATE 상태에서만 승인 가능
     * - INACTIVE 상태에서는 승인 불가
     */
    fun approve(
        adminId: Long,
        approvedTime: LocalDateTime = LocalDateTime.now()
    ) {
        require(adminId > 0) { "adminId must be valid" }

        when (status) {
            BrandPositionStatus.ACTIVE -> return

            BrandPositionStatus.INACTIVE ->
                throw IllegalStateException("Inactive BrandPosition cannot be approved")

            BrandPositionStatus.CANDIDATE -> {
                status = BrandPositionStatus.ACTIVE
                approvedAt = approvedTime
                approvedBy = adminId
            }
        }
    }

    /**
     * 비활성화 처리
     *
     * 도메인 규칙:
     * - ACTIVE / CANDIDATE → INACTIVE 허용
     * - 이미 INACTIVE면 no-op
     */
    fun deactivate() {
        if (status == BrandPositionStatus.INACTIVE) return
        status = BrandPositionStatus.INACTIVE
    }

    /**
     * 승인 여부
     *
     * 조회 전용 도메인 로직
     */
    fun isApproved(): Boolean =
        status == BrandPositionStatus.ACTIVE

    companion object {

        /**
         * BrandPosition 생성 팩토리
         *
         * 생성 규칙:
         * - 초기 상태는 반드시 CANDIDATE
         * - brandId / positionId / source 필수
         */
        fun create(
            brandId: Long,
            positionId: Long,
            displayName: String?,
            source: BrandPositionSource
        ): BrandPosition {

            require(brandId > 0) { "brandId must be positive" }
            require(positionId > 0) { "positionId must be positive" }

            return BrandPosition(
                brandId = brandId,
                positionId = positionId,
                displayName = displayName,
                status = BrandPositionStatus.CANDIDATE,
                source = source
            )
        }
    }
}
