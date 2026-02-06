package com.hirelog.api.position.application.port

import com.hirelog.api.common.application.port.PagedResult
import com.hirelog.api.position.application.view.PositionDetailView
import com.hirelog.api.position.application.view.PositionListView
import com.hirelog.api.position.application.view.PositionView
import org.springframework.data.domain.Pageable

/**
 * Position Query Port
 *
 * 책임:
 * - Position 조회 전용 계약
 * - Read Model(View) 반환
 *
 * 정책:
 * - Entity 반환 ❌
 * - 상태 변경 ❌
 */
interface PositionQuery {

    /**
     * Position 목록 조회 (관리자용)
     *
     * 특징:
     * - 핵심 정보만 조회
     * - Category 포함
     */
    fun findAll(
        status: String?,
        categoryId: Long?,
        name: String?,
        pageable: Pageable
    ): PagedResult<PositionListView>

    /**
     * Position 상세 조회
     */
    fun findDetailById(id: Long): PositionDetailView?

    /**
     * normalizedName으로 조회 (Read 전용)
     */
    fun findByNormalizedName(normalizedName: String): PositionView?

    /**
     * 활성화된 Position 이름 목록
     *
     * 용도:
     * - 자동완성
     * - 통계/분석
     */
    fun findActiveNames(): List<String>

    /**
     * 존재 여부 확인
     */
    fun existsById(id: Long): Boolean

    fun existsByNormalizedName(normalizedName: String): Boolean
}
