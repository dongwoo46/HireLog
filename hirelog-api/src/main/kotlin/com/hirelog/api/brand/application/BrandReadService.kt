package com.hirelog.api.brand.application

import com.hirelog.api.brand.application.port.BrandQuery
import com.hirelog.api.brand.application.view.BrandDetailView
import com.hirelog.api.brand.application.view.BrandListView
import com.hirelog.api.brand.presentation.controller.dto.BrandSearchReq
import com.hirelog.api.common.application.port.PagedResult
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

/**
 * Brand Read Application Service
 *
 * 책임:
 * - Brand 조회 유스케이스 전담
 * - 검색 / 상세 조회 오케스트레이션
 *
 * 비책임:
 * - 트랜잭션 관리 ❌
 * - 도메인 상태 변경 ❌
 * - 검증 로직 ❌
 */
@Service
class BrandReadService(
    private val brandQuery: BrandQuery
) {

    /**
     * 브랜드 목록 조회 (검색 + 페이징)
     */
    fun search(
        condition: BrandSearchReq,
        pageable: Pageable
    ): PagedResult<BrandListView> {

        return brandQuery.search(condition, pageable)
    }

    /**
     * 브랜드 상세 조회
     *
     * 정책:
     * - 존재하지 않으면 null 반환
     * - HTTP 404 변환은 Controller 책임
     */
    fun findDetail(brandId: Long): BrandDetailView? {
        return brandQuery.findDetailById(brandId)
    }
}
