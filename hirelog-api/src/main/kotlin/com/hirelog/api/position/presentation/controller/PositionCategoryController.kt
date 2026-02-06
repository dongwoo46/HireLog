package com.hirelog.api.position.presentation.controller

import com.hirelog.api.position.application.PositionCategoryWriteService
import com.hirelog.api.position.application.port.PositionCategoryQuery
import com.hirelog.api.position.application.view.PositionCategoryView
import com.hirelog.api.position.presentation.controller.dto.PositionCategoryCreateReq
import com.hirelog.api.common.application.port.PagedResult
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
    private val positionCategoryQuery: PositionCategoryQuery
) {

    /**
     * PositionCategory 생성
     */
    @PostMapping
    fun create(
        @Valid @RequestBody request: PositionCategoryCreateReq
    ): ResponseEntity<PositionCategoryView> {

        val category = positionCategoryWriteService.create(
            name = request.name,
            description = request.description
        )

        val view = positionCategoryQuery.findDetailById(category.id)
            ?: throw IllegalStateException("PositionCategory 생성 직후 조회 실패: ${category.id}")

        return ResponseEntity.status(HttpStatus.CREATED).body(view)
    }

    /**
     * PositionCategory 단건 조회
     */
    @GetMapping("/{categoryId}")
    fun getById(
        @PathVariable categoryId: Long
    ): ResponseEntity<PositionCategoryView> {

        val view = positionCategoryQuery.findDetailById(categoryId)
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(view)
    }

    /**
     * PositionCategory 목록 조회 (페이지네이션)
     */
    @GetMapping
    fun getAll(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<PagedResult<PositionCategoryView>> {

        val result = positionCategoryQuery.findAllPaged(page, size)
        return ResponseEntity.ok(result)
    }

    /**
     * PositionCategory 비활성화
     */
    @PostMapping("/{categoryId}/deactivate")
    fun deactivate(
        @PathVariable categoryId: Long
    ): ResponseEntity<Void> {

        positionCategoryWriteService.deactivate(categoryId)
        return ResponseEntity.ok().build()
    }
}
