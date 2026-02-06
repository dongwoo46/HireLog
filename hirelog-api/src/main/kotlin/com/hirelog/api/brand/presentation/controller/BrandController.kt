package com.hirelog.api.brand.presentation.controller

import com.hirelog.api.brand.application.command.BrandWriteService
import com.hirelog.api.brand.application.query.BrandQuery
import com.hirelog.api.brand.application.view.BrandDetailView
import com.hirelog.api.brand.application.view.BrandSummaryView
import com.hirelog.api.common.config.security.AuthenticatedMember
import com.hirelog.api.common.config.security.CurrentUser
import com.hirelog.api.common.logging.log
import com.hirelog.api.common.application.port.PagedResult
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

/**
 * Brand Controller
 *
 * 책임:
 * - Brand 조회 API 제공
 * - Brand 상태 변경 트리거 (관리자)
 *
 * 비책임:
 * - 비즈니스 로직 ❌
 * - 트랜잭션 관리 ❌
 */
@RestController
@RequestMapping("/api/brands")
class BrandController(
    private val brandQuery: BrandQuery,
    private val brandWriteService: BrandWriteService
) {

    /**
     * 브랜드 목록 조회 (페이지네이션)
     *
     * GET /api/brands
     */
    @GetMapping
    fun getBrands(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<PagedResult<BrandSummaryView>> {

        val result = brandQuery.findAllPaged(page, size)
        return ResponseEntity.ok(result)
    }

    /**
     * 브랜드 상세 조회
     *
     * GET /api/brands/{brandId}
     */
    @GetMapping("/{brandId}")
    fun getBrandDetail(
        @PathVariable brandId: Long
    ): ResponseEntity<BrandDetailView> {

        val brand = brandQuery.findDetailById(brandId)
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(brand)
    }

    /**
     * 브랜드 검증 승인 (관리자)
     *
     * PATCH /api/brands/{brandId}/verify
     */
    @PatchMapping("/{brandId}/verify")
    @PreAuthorize("hasRole('ADMIN')")
    fun verifyBrand(
        @CurrentUser member: AuthenticatedMember,
        @PathVariable brandId: Long
    ): ResponseEntity<Void> {

        log.info(
            "[BrandVerify] adminId={}, brandId={}",
            member.memberId,
            brandId
        )

        brandWriteService.verify(brandId)
        return ResponseEntity.noContent().build()
    }

    /**
     * 브랜드 검증 거절 (관리자)
     *
     * PATCH /api/brands/{brandId}/reject
     */
    @PatchMapping("/{brandId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    fun rejectBrand(
        @CurrentUser member: AuthenticatedMember,
        @PathVariable brandId: Long
    ): ResponseEntity<Void> {

        log.info(
            "[BrandReject] adminId={}, brandId={}",
            member.memberId,
            brandId
        )

        brandWriteService.reject(brandId)
        return ResponseEntity.noContent().build()
    }

    /**
     * 브랜드 비활성화 (관리자)
     *
     * PATCH /api/brands/{brandId}/deactivate
     */
    @PatchMapping("/{brandId}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    fun deactivateBrand(
        @CurrentUser member: AuthenticatedMember,
        @PathVariable brandId: Long
    ): ResponseEntity<Void> {

        log.info(
            "[BrandDeactivate] adminId={}, brandId={}",
            member.memberId,
            brandId
        )

        brandWriteService.deactivate(brandId)
        return ResponseEntity.noContent().build()
    }
}
