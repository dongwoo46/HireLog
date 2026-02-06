package com.hirelog.api.position.application

import com.hirelog.api.common.application.port.PagedResult
import com.hirelog.api.position.application.port.PositionQuery
import com.hirelog.api.position.application.view.PositionDetailView
import com.hirelog.api.position.application.view.PositionListView
import com.hirelog.api.position.application.view.PositionView
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Position Read Application Service
 *
 * 책임:
 * - Position 조회 유스케이스 제공
 * - Controller와 Query Port 사이의 중간 계층
 *
 * 특징:
 * - Read Only
 * - 트랜잭션은 읽기 전용
 * - Entity 반환 ❌
 */
@Service
@Transactional(readOnly = true)
class PositionReadService(
    private val positionQuery: PositionQuery
) {

    /**
     * Position 목록 조회 (검색 + 페이징)
     *
     * 검색 조건:
     * - status
     * - categoryId
     * - name (부분 일치)
     */
    fun search(
        status: String?,
        categoryId: Long?,
        name: String?,
        pageable: Pageable
    ): PagedResult<PositionListView> {
        return positionQuery.findAll(
            status = status,
            categoryId = categoryId,
            name = name,
            pageable = pageable
        )
    }

    /**
     * Position 상세 조회
     */
    fun findDetail(positionId: Long): PositionDetailView? {
        return positionQuery.findDetailById(positionId)
    }

    /**
     * normalizedName 기준 단건 조회
     *
     * 용도:
     * - 내부 매핑
     * - 검증 로직
     */
    fun findByNormalizedName(normalizedName: String): PositionView? {
        return positionQuery.findByNormalizedName(normalizedName)
    }

    /**
     * 활성화된 Position 이름 목록 조회
     *
     * 용도:
     * - LLM 프롬프트
     * - 자동 매핑
     */
    fun findActiveNames(): List<String> {
        return positionQuery.findActiveNames()
    }
}
