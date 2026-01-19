package com.hirelog.api.position.domain

import com.hirelog.api.common.jpa.BaseEntity
import jakarta.persistence.*

@Entity
@Table(
    name = "position",
    indexes = [
        Index(
            name = "idx_position_normalized_name",
            columnList = "normalized_name",
            unique = true
        ),
        Index(
            name = "idx_position_status",
            columnList = "status"
        )
    ]
)
class Position(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    /**
     * 시장 공통 포지션명
     * 예: Backend Engineer, Frontend Engineer
     */
    @Column(name = "name", nullable = false, length = 200)
    val name: String,

    /**
     * 정규화된 포지션명
     * - 소문자
     * - 공백/기호 제거
     * - 중복 방지 및 매핑 기준
     * 예: backend_engineer
     */
    @Column(name = "normalized_name", nullable = false, length = 200)
    val normalizedName: String,

    /**
     * 포지션 상태
     * - ACTIVE: 정식 포지션
     * - CANDIDATE: 신규/검증 대기
     * - DEPRECATED: 더 이상 사용하지 않음
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: PositionStatus = PositionStatus.ACTIVE,

    /**
     * 포지션 설명 (선택)
     * - UI 표시
     * - 관리용
     */
    @Column(name = "description", length = 500)
    val description: String? = null

) : BaseEntity() {
    companion object {

        /**
         * Position 생성 팩토리
         *
         * 역할:
         * - 포지션 생성 정책을 강제
         * - 최초 상태를 CANDIDATE로 고정
         */
        fun create(
            name: String,
            normalizedName: String,
            description: String?
        ): Position {
            return Position(
                name = name,
                normalizedName = normalizedName,
                status = PositionStatus.CANDIDATE,
                description = description
            )
        }
    }

    /**
     * 포지션 활성화
     *
     * 역할:
     * - 검증 완료된 포지션을 정식 포지션으로 전환
     */
    fun activate() {
        if (status == PositionStatus.ACTIVE) return
        status = PositionStatus.ACTIVE
    }

    /**
     * 포지션 비활성화 (Deprecated)
     *
     * 역할:
     * - 더 이상 사용하지 않는 포지션 처리
     */
    fun deprecate() {
        if (status == PositionStatus.DEPRECATED) return
        status = PositionStatus.DEPRECATED
    }
}
