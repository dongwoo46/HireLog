package com.hirelog.api.relation.presentation.controller

import com.hirelog.api.common.application.port.PagedResult
import com.hirelog.api.common.config.security.AuthenticatedMember
import com.hirelog.api.common.config.security.CurrentUser
import com.hirelog.api.relation.application.brandposition.BrandPositionReadService
import com.hirelog.api.relation.application.brandposition.BrandPositionWriteService
import com.hirelog.api.relation.application.brandposition.view.BrandPositionListView
import com.hirelog.api.relation.presentation.controller.dto.BrandPositionCreateReq
import com.hirelog.api.relation.presentation.controller.dto.BrandPositionDisplayNameChangeReq
import com.hirelog.api.relation.presentation.controller.dto.BrandPositionSearchReq
import com.hirelog.api.relation.presentation.controller.dto.BrandPositionStatusChangeReq
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

/**
 * BrandPosition Controller
 *
 * 정책:
 * - 상세 조회 API 없음
 * - 목록 조회 + 상태 변경 중심
 */
@RestController
@RequestMapping("/api/brand-position")
@PreAuthorize("hasRole('ADMIN')")
class BrandPositionController(
    private val brandPositionReadService: BrandPositionReadService,
    private val brandPositionWriteService: BrandPositionWriteService
) {

    /**
     * BrandPosition 명시적 생성 (Admin)
     */
    @PostMapping
    fun create(
        @Valid @RequestBody request: BrandPositionCreateReq,
        @CurrentUser admin: AuthenticatedMember
    ): ResponseEntity<Void> {

        brandPositionWriteService.create(
            brandId = request.brandId,
            positionId = request.positionId,
            displayName = request.displayName,
            source = request.source
        )

        return ResponseEntity.noContent().build()
    }

    /**
     * BrandPosition 목록 조회 (검색 + 페이징)
     *
     * GET /api/brand-positions
     */
    @GetMapping
    fun search(
        @ModelAttribute condition: BrandPositionSearchReq,
        @PageableDefault(size = 20) pageable: Pageable,
        @CurrentUser member: AuthenticatedMember
    ): ResponseEntity<PagedResult<BrandPositionListView>> {

        val result = brandPositionReadService.search(
            condition = condition,
            pageable = pageable
        )

        return ResponseEntity.ok(result)
    }

    /**
     * BrandPosition 상태 변경
     *
     * - ACTIVE / INACTIVE / CANDIDATE 모두 허용
     */
    @PatchMapping("/{brandPositionId}/status")
    fun changeStatus(
        @PathVariable brandPositionId: Long,
        @Valid @RequestBody request: BrandPositionStatusChangeReq,
        @CurrentUser admin: AuthenticatedMember
    ): ResponseEntity<Void> {

        brandPositionWriteService.changeStatus(
            brandPositionId = brandPositionId,
            newStatus = request.status,
            adminId = admin.memberId
        )

        return ResponseEntity.noContent().build()
    }


    /**
     * BrandPosition displayName 변경
     */
    @PatchMapping("/{brandPositionId}/display-name")
    fun changeDisplayName(
        @PathVariable brandPositionId: Long,
        @Valid @RequestBody request: BrandPositionDisplayNameChangeReq
    ): ResponseEntity<Void> {

        brandPositionWriteService.changeDisplayName(
            brandPositionId = brandPositionId,
            newDisplayName = request.displayName
        )

        return ResponseEntity.noContent().build()
    }
}
