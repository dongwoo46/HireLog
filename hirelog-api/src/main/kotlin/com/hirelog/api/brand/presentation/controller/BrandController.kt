package com.hirelog.api.brand.presentation.controller

import com.hirelog.api.brand.application.BrandReadService
import com.hirelog.api.brand.application.BrandWriteService
import com.hirelog.api.brand.application.view.BrandDetailView
import com.hirelog.api.brand.application.view.BrandListView
import com.hirelog.api.brand.presentation.controller.dto.BrandCreateReq
import com.hirelog.api.brand.presentation.controller.dto.BrandSearchReq
import com.hirelog.api.common.application.port.PagedResult
import com.hirelog.api.common.config.security.AuthenticatedMember
import com.hirelog.api.common.config.security.CurrentUser
import com.hirelog.api.common.logging.log
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

/**
 * Brand Controller
 *
 * 책임:
 * - Brand 목록/상세 조회
 * - Brand 상태 변경 트리거 (관리자)
 *
 * 비책임:
 * - 비즈니스 로직 ❌
 * - 트랜잭션 관리 ❌
 */
@RestController
@RequestMapping("/api/brand")
class BrandController(
    private val brandReadService: BrandReadService,
    private val brandWriteService: BrandWriteService
) {

    /**
     * 브랜드 생성 (관리자)
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    fun create(
        @Valid @RequestBody request: BrandCreateReq,
    ): ResponseEntity<Void> {

        brandWriteService.create(
            name = request.name,
            companyId = request.companyId,
        )

        return ResponseEntity.noContent().build()
    }

    /**
     * 브랜드 목록 조회 (검색 + 페이징)
     *
     * GET /api/brands
     */
    @GetMapping
    fun search(
        @ModelAttribute condition: BrandSearchReq,
        @PageableDefault(size = 20) pageable: Pageable,
        @CurrentUser member: AuthenticatedMember
    ): ResponseEntity<PagedResult<BrandListView>> {

        val result = brandReadService.search(condition, pageable)
        return ResponseEntity.ok(result)
    }

    /**
     * 브랜드 상세 조회
     *
     * GET /api/brands/{brandId}
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{brandId}")
    fun getDetail(
        @PathVariable brandId: Long,
        @CurrentUser member: AuthenticatedMember
    ): ResponseEntity<BrandDetailView> {

        val view = brandReadService.findDetail(brandId)
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(view)
    }

    /**
     * 브랜드 검증 승인
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{brandId}/verify")
    fun verify(
        @PathVariable brandId: Long,
    ): ResponseEntity<Void> {

        brandWriteService.verify(brandId)
        return ResponseEntity.noContent().build()
    }

    /**
     * 브랜드 검증 거절
     */
    @PatchMapping("/{brandId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    fun reject(
        @PathVariable brandId: Long,
    ): ResponseEntity<Void> {

        brandWriteService.reject(brandId)
        return ResponseEntity.noContent().build()
    }

    /**
     * 브랜드 활성화
     */
    @PatchMapping("/{brandId}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    fun activate(
        @PathVariable brandId: Long,
    ): ResponseEntity<Void> {

        brandWriteService.activate(brandId)
        return ResponseEntity.noContent().build()
    }

    /**
     * 브랜드 비활성화
     */
    @PatchMapping("/{brandId}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    fun deactivate(
        @PathVariable brandId: Long,
    ): ResponseEntity<Void> {

        brandWriteService.deactivate(brandId)
        return ResponseEntity.noContent().build()
    }
}
