package com.hirelog.api.relation.domain.model

import com.hirelog.api.common.infra.jpa.entity.BaseEntity
import com.hirelog.api.relation.domain.type.BrandPositionSource
import com.hirelog.api.relation.domain.type.BrandPositionStatus
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
    @Column(name = "display_name", length = 200, nullable = false)
    var displayName: String,

    /**
     * 브랜드 기준 포지션 상태
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: BrandPositionStatus = BrandPositionStatus.ACTIVE,

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
     * BrandPosition 상태 변경
     *
     * 도메인 규칙:
     * - 모든 상태로의 변경은 이 메서드를 통해서만 가능
     * - 승인 메타데이터는 ACTIVE 전환 시에만 설정
     */
    fun changeStatus(
        newStatus: BrandPositionStatus,
        adminId: Long? = null,
        changedTime: LocalDateTime = LocalDateTime.now()
    ) {
        if (this.status == newStatus) return

        when (newStatus) {

            BrandPositionStatus.ACTIVE -> {
                require(adminId != null && adminId > 0) {
                    "adminId is required when activating BrandPosition"
                }

                this.status = BrandPositionStatus.ACTIVE
                this.approvedAt = changedTime
                this.approvedBy = adminId
            }

            BrandPositionStatus.INACTIVE -> {
                this.status = BrandPositionStatus.INACTIVE
            }

            BrandPositionStatus.CANDIDATE -> {
                this.status = BrandPositionStatus.CANDIDATE
                this.approvedAt = null
                this.approvedBy = null
            }
        }
    }


    /**
     * 브랜드 포지션 표시명 변경
     *
     * 정책:
     * - null 허용 (공통 Position.name fallback)
     * - 빈 문자열 불가
     * - 동일 값 변경은 no-op
     */
    fun changeDisplayName(newDisplayName: String) {
        require(newDisplayName.isNotBlank()) {
            "displayName must not be blank"
        }

        if (this.displayName == newDisplayName) return

        this.displayName = newDisplayName
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
            displayName: String,
            source: BrandPositionSource
        ): BrandPosition {

            require(brandId > 0) { "brandId must be positive" }
            require(positionId > 0) { "positionId must be positive" }

            return BrandPosition(
                brandId = brandId,
                positionId = positionId,
                displayName = displayName,
                status = BrandPositionStatus.ACTIVE,
                source = source
            )
        }
    }
}
