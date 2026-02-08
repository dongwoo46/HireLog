package com.hirelog.api.relation.application.brandposition

import com.hirelog.api.common.application.port.PagedResult
import com.hirelog.api.relation.application.brandposition.port.BrandPositionQuery
import com.hirelog.api.relation.application.brandposition.view.BrandPositionListView
import com.hirelog.api.relation.presentation.controller.dto.BrandPositionSearchReq
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * BrandPosition Read Application Service
 *
 * 책임:
 * - BrandPosition 조회 유스케이스 전담
 * - Query Port 위임
 *
 * 원칙:
 * - 도메인 엔티티 접근 ❌
 * - 트랜잭션은 readOnly
 * - View Model만 반환
 */
@Service
@Transactional(readOnly = true)
class BrandPositionReadService(
    private val brandPositionQuery: BrandPositionQuery
) {

    /**
     * BrandPosition 목록 조회 (검색 + 페이징)
     */
    fun search(
        condition: BrandPositionSearchReq,
        pageable: Pageable
    ): PagedResult<BrandPositionListView> {

        return brandPositionQuery.search(
            condition = condition,
            pageable = pageable
        )
    }
}
