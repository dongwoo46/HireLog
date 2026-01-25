package com.hirelog.api.position.application.query

import com.hirelog.api.position.domain.PositionStatus

/**
 * PositionView
 *
 * Position 조회 전용 Read Model
 *
 * 역할:
 * - Position 엔티티를 외부(API/UI)에 직접 노출하지 않기 위한 Projection
 * - 조회/검색/추천/통계의 기준 데이터
 *
 * 설계 원칙:
 * - 불변 객체
 * - 비즈니스 로직 없음
 * - 필요한 필드만 포함
 */
data class PositionView(

    /**
     * Position 식별자
     */
    val id: Long,

    /**
     * 대표 포지션명
     * 예: Backend Engineer
     */
    val name: String,

    /**
     * 정규화된 포지션명
     *
     * 예: backend_engineer
     *
     * 용도:
     * - 내부 매핑
     * - 중복 판별
     * - 안정적인 기준 키
     */
    val normalizedName: String,

    /**
     * 포지션 상태
     */
    val status: PositionStatus,

    /**
     * 포지션 설명
     *
     * - UI 표시용
     * - 관리자 참고용
     */
    val description: String?
)
