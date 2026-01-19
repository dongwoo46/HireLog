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
class BrandPosition(

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
     * 브랜드/회사 내부에서 사용하는 포지션명
     * 예: Server Engineer, Platform Backend Engineer
     *
     * - null이면 Position.name 사용
     */
    @Column(name = "display_name", length = 200)
    val displayName: String? = null,

    /**
     * 브랜드 기준 포지션 상태
     *
     * CANDIDATE : LLM 자동 생성 / 검증 대기
     * ACTIVE    : 관리자 승인 완료 + 사용 중
     * INACTIVE  : 더 이상 사용하지 않음
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: BrandPositionStatus = BrandPositionStatus.CANDIDATE,

    /**
     * 생성 출처
     * - LLM   : JD 분석 기반 자동 생성
     * - ADMIN : 관리자가 직접 생성
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 20)
    val source: BrandPositionSource,

    /**
     * 관리자 승인 시각
     * - status가 ACTIVE로 전환된 시점
     */
    @Column(name = "approved_at")
    var approvedAt: LocalDateTime? = null,

    /**
     * 승인한 관리자 ID
     */
    @Column(name = "approved_by")
    var approvedBy: Long? = null

) : BaseEntity() {

    /**
     * 관리자 승인 처리
     *
     * 역할:
     * - 상태를 ACTIVE로 변경
     * - 승인 시각 및 관리자 정보 기록
     *
     * idempotent:
     * - 이미 ACTIVE면 아무 작업도 하지 않음
     */
    fun approve(adminId: Long, approvedTime: LocalDateTime = LocalDateTime.now()) {
        if (status == BrandPositionStatus.ACTIVE) return

        status = BrandPositionStatus.ACTIVE
        approvedAt = approvedTime
        approvedBy = adminId
    }

    /**
     * 비활성화 처리
     *
     * 역할:
     * - 더 이상 사용하지 않는 포지션으로 전환
     * - 과거 데이터 보존 목적
     */
    fun deactivate() {
        if (status == BrandPositionStatus.INACTIVE) return
        status = BrandPositionStatus.INACTIVE
    }
}
