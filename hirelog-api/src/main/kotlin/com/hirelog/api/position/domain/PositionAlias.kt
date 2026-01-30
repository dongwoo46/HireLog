package com.hirelog.api.position.domain

import com.hirelog.api.common.infra.jpa.entity.BaseEntity
import jakarta.persistence.*

/**
 * PositionAlias
 *
 * 외부 시스템 또는 비정형 입력에서 사용되는 다양한 직무/직업 명칭을
 * HireLog 내부의 공통 Position으로 매핑하기 위한 보조 도메인 엔티티.
 *
 * 개념적 역할:
 * - Position : HireLog가 정의한 "기준 직무"
 * - PositionAlias : 외부 표현을 내부 기준(Position)으로 해석하기 위한 "별칭"
 *
 * 예시:
 * - "Java 개발자"
 * - "서버 개발자"
 * - "Backend Engineer (Java)"
 *
 * 특징:
 * - 자동 생성 가능 (LLM, 크롤링, 외부 공공 데이터 등)
 * - 관리자 검증 단계를 거침
 * - 언제든 비활성화 가능 (물리 삭제 ❌)
 *
 * 설계 원칙:
 * - PositionAlias 자체는 비즈니스 기준이 아니다.
 * - 통계, 분석, 검색의 기준은 항상 Position이다.
 * - 신뢰되지 않은 외부 표현을 안전하게 흡수하기 위한 완충 계층이다.
 */
@Entity
@Table(
    name = "position_alias",
    indexes = [
        /**
         * 정규화된 별칭명 기준 인덱스
         *
         * - alias 중복 방지
         * - 빠른 매핑 조회 목적
         */
        Index(
            name = "idx_position_alias_normalized",
            columnList = "normalized_alias_name",
            unique = true
        ),

        /**
         * 별칭 상태 기준 인덱스
         *
         * - ACTIVE 별칭만 조회
         * - 관리자 검증 대기(PENDING) 목록 관리
         */
        Index(
            name = "idx_position_alias_status",
            columnList = "status"
        )
    ]
)
class PositionAlias protected constructor(

    /**
     * 내부 식별자
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    /**
     * 외부에서 유입된 원본 직무/직업 명칭
     *
     * 예:
     * - "Java 개발자"
     * - "서버 개발자"
     *
     * 주의:
     * - 사용자 입력 또는 외부 데이터의 원형을 그대로 보존한다.
     * - 이 값 자체로는 매핑 기준으로 사용하지 않는다.
     */
    @Column(name = "alias_name", nullable = false, length = 200)
    val aliasName: String,

    /**
     * aliasName을 시스템 기준으로 정규화한 값
     *
     * 역할:
     * - 중복 방지 키
     * - 외부 표현 매핑의 기준값
     *
     * 규칙:
     * - 소문자 변환
     * - 특수문자 제거
     * - 공백 및 구분자 → underscore
     *
     * 예:
     * - "Java 개발자" → "java_developer"
     */
    @Column(
        name = "normalized_alias_name",
        nullable = false,
        length = 200,
        unique = true
    )
    val normalizedAliasName: String,

    /**
     * 이 별칭이 귀속되는 대표 Position
     *
     * 의미:
     * - 이 alias는 궁극적으로 어떤 공통 Position을 의미하는가
     *
     * 제약:
     * - 하나의 alias는 하나의 Position에만 귀속된다 (1:N 구조)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "position_id", nullable = false)
    val position: Position,

    /**
     * 별칭 상태
     *
     * 생명주기:
     * - PENDING  : 자동 생성 / 아직 검증되지 않음
     * - ACTIVE   : 관리자 승인 완료, 매핑에 사용 가능
     * - INACTIVE : 잘못된 표현 또는 더 이상 사용하지 않음
     *
     * 정책:
     * - 매핑 로직에서는 항상 ACTIVE 상태만 사용한다.
     * - 삭제 대신 상태 전이를 사용하여 히스토리를 보존한다.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: PositionAliasStatus

) : BaseEntity() {

    companion object {

        /**
         * 자동 생성용 팩토리 메서드
         *
         * 사용 시점:
         * - LLM 추론 결과
         * - 크롤링 데이터
         * - 공공 데이터 수집
         *
         * 정책:
         * - 항상 PENDING 상태로 생성된다.
         * - 관리자 승인 전까지 매핑에 사용되지 않는다.
         */
        fun createPending(
            aliasName: String,
            position: Position
        ): PositionAlias {
            return PositionAlias(
                aliasName = aliasName,
                normalizedAliasName = normalize(aliasName),
                position = position,
                status = PositionAliasStatus.PENDING
            )
        }

        /**
         * 관리자 수동 생성용 팩토리 메서드
         *
         * 사용 시점:
         * - 관리자가 명확한 의도를 가지고 직접 등록한 경우
         *
         * 정책:
         * - 즉시 ACTIVE 상태로 생성된다.
         */
        fun createActive(
            aliasName: String,
            position: Position
        ): PositionAlias {
            return PositionAlias(
                aliasName = aliasName,
                normalizedAliasName = normalize(aliasName),
                position = position,
                status = PositionAliasStatus.ACTIVE
            )
        }

        /**
         * aliasName 정규화 규칙
         *
         * 책임:
         * - 외부 표현을 시스템 기준 키로 변환
         *
         * 주의:
         * - 외부에서 normalized 값을 직접 생성하지 못하도록
         *   도메인 내부에 캡슐화한다.
         */
        internal fun normalize(value: String): String =
            value
                .lowercase()
                .replace(Regex("[^a-z0-9]+"), "_")
                .trim('_')
    }

    /**
     * 별칭 승인
     *
     * 역할:
     * - 관리자 검증 완료 처리
     * - PENDING → ACTIVE 전이만 허용
     */
    fun approve() {
        if (status != PositionAliasStatus.PENDING) return
        status = PositionAliasStatus.ACTIVE
    }

    /**
     * 별칭 비활성화
     *
     * 역할:
     * - 잘못된 매핑 제거
     * - 더 이상 사용하지 않는 표현 처리
     *
     * 정책:
     * - 물리 삭제 ❌
     * - 상태 전이로 히스토리 유지
     */
    fun deactivate() {
        if (status == PositionAliasStatus.INACTIVE) return
        status = PositionAliasStatus.INACTIVE
    }
}
