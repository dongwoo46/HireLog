package com.hirelog.api.position.presentation.controller

import com.hirelog.api.position.application.PositionCategoryWriteService
import com.hirelog.api.position.presentation.controller.dto.PositionCategoryCreateReq
import com.hirelog.api.common.application.port.PagedResult
import com.hirelog.api.position.application.PositionCategoryReadService
import com.hirelog.api.position.application.view.PositionCategoryDetailView
import com.hirelog.api.position.application.view.PositionCategoryListView
import com.hirelog.api.position.presentation.controller.dto.PositionCategorySearchReq
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/position-category")
@PreAuthorize("hasRole('ADMIN')")
class PositionCategoryController(
    private val positionCategoryWriteService: PositionCategoryWriteService,
    private val positionCategoryReadService: PositionCategoryReadService
) {

    /**
     * PositionCategory 생성
     */
    @PostMapping
    fun create(
        @Valid @RequestBody request: PositionCategoryCreateReq
    ): ResponseEntity<PositionCategoryDetailView> {

        positionCategoryWriteService.create(
            name = request.name,
            description = request.description
        )

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .build()
    }

    /**
     * PositionCategory 단건 조회
     */
    @GetMapping("/{categoryId}")
    fun getById(
        @PathVariable categoryId: Long
    ): ResponseEntity<PositionCategoryDetailView> {

        val view = positionCategoryReadService.findDetail(categoryId)
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(view)
    }

    /**
     * PositionCategory 목록 조회 (검색 + 페이지네이션)
     */
    @GetMapping
    fun search(
        @ModelAttribute condition: PositionCategorySearchReq
    ): ResponseEntity<PagedResult<PositionCategoryListView>> {

        val result = positionCategoryReadService.search(
            status = condition.status,
            name = condition.name,
            page = condition.page,
            size = condition.size
        )

        return ResponseEntity.ok(result)
    }

    /**
     * PositionCategory 활성화
     */
    @PatchMapping("/{categoryId}/activate")
    fun activate(
        @PathVariable categoryId: Long
    ): ResponseEntity<Void> {

        positionCategoryWriteService.activate(categoryId)
        return ResponseEntity.ok().build()
    }

    /**
     * PositionCategory 비활성화
     */
    @PatchMapping("/{categoryId}/deactivate")
    fun deactivate(
        @PathVariable categoryId: Long
    ): ResponseEntity<Void> {

        positionCategoryWriteService.deactivate(categoryId)
        return ResponseEntity.ok().build()
    }
}
