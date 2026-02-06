package com.hirelog.api.position.domain

import com.hirelog.api.common.infra.jpa.entity.BaseEntity
import jakarta.persistence.*

/**
 * Position
 *
 * HireLog 내부에서 정의한 "공통 포지션 기준" 엔티티.
 *
 * 개념적 의미:
 * - 외부 데이터(워크넷, JD, LLM, 크롤링 등)가
 *   최종적으로 귀결되는 내부 기준 직무
 *
 * 역할:
 * - 검색, 통계, 분석, 트렌드 산출의 기준점
 * - 다양한 외부 표현(PositionAlias)을 하나의 의미로 통합
 *
 * 특징:
 * - 개수는 제한적이며 사람이 직접 정의/관리한다.
 * - 자동 생성되지 않는다.
 * - HireLog 도메인의 핵심 자산이다.
 *
 * 설계 원칙:
 * - 표현(name)과 식별자(normalizedName)를 분리한다.
 * - 외부 데이터는 Position을 직접 생성/수정할 수 없다.
 */
@Entity
@Table(
    name = "position",
    indexes = [
        /**
         * 시스템 식별용 정규화 포지션명 인덱스
         *
         * - 중복 방지
         * - 매핑 기준
         */
        Index(
            name = "idx_position_normalized_name",
            columnList = "normalized_name",
            unique = true
        ),

        /**
         * 포지션 상태 인덱스
         *
         * - ACTIVE 포지션 조회
         * - 운영/관리 화면 필터링
         */
        Index(
            name = "idx_position_status",
            columnList = "status"
        )
    ]
)
class Position protected constructor(

    /**
     * 내부 식별자
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    /**
     * 사람이 인식하는 포지션명
     *
     * 예:
     * - "Backend Engineer"
     * - "Data Engineer"
     *
     * 주의:
     * - UI 및 외부 노출용
     * - 변경 가능
     */
    @Column(name = "name", nullable = false, length = 200)
    val name: String,

    /**
     * 시스템 식별용 정규화 포지션명
     *
     * 역할:
     * - Position의 시스템 내부 식별자
     * - 중복 방지 기준
     * - 외부 데이터 매핑 기준
     *
     * 규칙:
     * - 소문자
     * - 특수문자 제거
     * - 공백/구분자 → underscore
     *
     * 예:
     * - "Backend Engineer" → "backend_engineer"
     */
    @Column(name = "normalized_name", nullable = false, length = 200)
    val normalizedName: String,

    /**
     * 포지션 상태
     *
     * 생명주기:
     * - ACTIVE     : 정식 사용
     * - INACTVE : 더 이상 사용하지 않음
     *
     * 정책:
     * - 외부 데이터 매핑은 ACTIVE 상태만 사용한다.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: PositionStatus,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    val category: PositionCategory,

    /**
     * 포지션 설명
     *
     * 용도:
     * - 관리자 관리
     * - UI 보조 설명
     */
    @Column(name = "description", length = 500)
    val description: String? = null

) : BaseEntity() {

    companion object {

        /**
         * Position 생성 팩토리
         *
         * 사용 시점:
         * - 관리자가 새로운 공통 포지션을 정의할 때
         *
         * 정책:
         * - 최초 상태는 항상 CANDIDATE
         * - normalizedName은 도메인에서 자동 생성
         */
        fun create(
            name: String,
            description: String?,
            positionCategory: PositionCategory
        ): Position {
            return Position(
                name = name,
                normalizedName = normalize(name),
                status = PositionStatus.ACTIVE,
                description = description,
                category = positionCategory
            )
        }

        /**
         * 포지션명 정규화 규칙
         *
         * 책임:
         * - 사람이 정의한 포지션명을
         *   시스템 식별자 형태로 변환
         *
         * 주의:
         * - 외부에서 normalizedName을 직접 지정하지 못하도록
         *   도메인 내부에 캡슐화한다.
         */
        private fun normalize(value: String): String =
            value
                .lowercase()
                .replace(Regex("[^a-z0-9]+"), "_")
                .trim('_')
    }

    /**
     * 포지션 활성화
     *
     * 역할:
     * - 검증 완료된 포지션을 정식 포지션으로 전환
     *
     * 상태 전이:
     * - CANDIDATE → ACTIVE
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
     *
     * 정책:
     * - 물리 삭제 ❌
     * - 상태 전이로 히스토리 유지
     */
    fun deprecate() {
        if (status == PositionStatus.INACTIVE) return
        status = PositionStatus.INACTIVE
    }
}
