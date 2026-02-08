package com.hirelog.api.relation.infra.persistence.jpa.adapter

import com.hirelog.api.common.application.port.PagedResult
import com.hirelog.api.relation.application.brandposition.port.BrandPositionQuery
import com.hirelog.api.relation.application.brandposition.view.BrandPositionListView
import com.hirelog.api.relation.infra.persistence.jpa.repository.BrandPositionJpaQueryDsl
import com.hirelog.api.relation.infra.persistence.jpa.repository.BrandPositionJpaRepository
import com.hirelog.api.relation.presentation.controller.dto.BrandPositionSearchReq
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

/**
 * BrandPositionQueryJpaAdapter
 *
 * 책임:
 * - BrandPosition 조회 전용 Adapter
 * - QueryDSL 기반 Read Model 조회
 *
 * 제약:
 * - 도메인 엔티티 반환 ❌
 * - 쓰기 로직 ❌
 */
@Component
class BrandPositionJpaQuery(
    private val queryDsl: BrandPositionJpaQueryDsl,
    private val repository: BrandPositionJpaRepository   // ✅ 추가
) : BrandPositionQuery {

    override fun search(
        condition: BrandPositionSearchReq,
        pageable: Pageable
    ): PagedResult<BrandPositionListView> {

        val (content, total) = queryDsl.search(
            condition = condition,
            pageable = pageable
        )

        return PagedResult.of(
            items = content,
            page = pageable.pageNumber,
            size = pageable.pageSize,
            totalElements = total
        )
    }

    override fun existsByBrandIdAndPositionId(
        brandId: Long,
        positionId: Long
    ): Boolean {
        return repository.existsByBrandIdAndPositionId(
            brandId = brandId,
            positionId = positionId
        )
    }

    override fun existsById(id: Long): Boolean {
        return repository.existsById(id)
    }
}
