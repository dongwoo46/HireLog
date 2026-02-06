package com.hirelog.api.position.presentation.controller

import com.hirelog.api.common.application.port.PagedResult
import com.hirelog.api.common.config.security.AuthenticatedMember
import com.hirelog.api.common.config.security.CurrentUser
import com.hirelog.api.member.application.view.PositionView
import com.hirelog.api.position.application.PositionReadService
import com.hirelog.api.position.application.PositionWriteService
import com.hirelog.api.position.application.view.PositionDetailView
import com.hirelog.api.position.application.view.PositionListView
import com.hirelog.api.position.presentation.controller.dto.PositionCreateReq
import com.hirelog.api.position.presentation.controller.dto.PositionSearchReq
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/position")
@PreAuthorize("hasRole('ADMIN')")
class PositionController(
    private val positionWriteService: PositionWriteService,
    private val positionReadService: PositionReadService
) {

    /**
     * Position 생성
     *
     * 정책:
     * - 관리자만 생성 가능
     * - normalizedName은 도메인에서 생성
     */
    @PostMapping
    fun create(
        @Valid @RequestBody request: PositionCreateReq,
    ): ResponseEntity<Void> {

        positionWriteService.create(
            name = request.name,
            categoryId = request.categoryId,
            description = request.description,
        )

        return ResponseEntity.status(HttpStatus.CREATED).build()
    }

    /**
     * Position 목록 조회 (검색 + 페이징)
     *
     * 조회 범위:
     * - 핵심 정보만 반환
     * - Category 포함
     */
    @GetMapping
    fun search(
        @ModelAttribute condition: PositionSearchReq,
        @PageableDefault(size = 20) pageable: Pageable
    ): ResponseEntity<PagedResult<PositionListView>> {

        val result = positionReadService.search(
            status = condition.status,
            categoryId = condition.categoryId,
            name = condition.name,
            pageable = pageable
        )

        return ResponseEntity.ok(result)
    }

    /**
     * Position 상세 조회
     *
     * 조회 범위:
     * - Position 전체 정보
     * - Category 포함
     */
    @GetMapping("/{positionId}")
    fun getDetail(
        @PathVariable positionId: Long
    ): ResponseEntity<PositionDetailView> {

        val view = positionReadService.findDetail(positionId)
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(view)
    }

    /**
     * Position 활성화
     */
    @PatchMapping("/{positionId}/activate")
    fun activate(
        @PathVariable positionId: Long,
    ): ResponseEntity<Void> {

        positionWriteService.activate(positionId)
        return ResponseEntity.ok().build()
    }

    /**
     * Position 비활성화
     */
    @PatchMapping("/{positionId}/deactivate")
    fun deactivate(
        @PathVariable positionId: Long,
    ): ResponseEntity<Void> {

        positionWriteService.deprecate(positionId)
        return ResponseEntity.ok().build()
    }
}
