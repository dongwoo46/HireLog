package com.hirelog.api.position.presentation.controller

import com.hirelog.api.common.exception.EntityNotFoundException
import com.hirelog.api.position.application.PositionWriteService
import com.hirelog.api.position.application.port.PositionCategoryCommand
import com.hirelog.api.position.application.port.PositionQuery
import com.hirelog.api.position.application.view.PositionDetailView
import com.hirelog.api.position.application.view.PositionSummaryView
import com.hirelog.api.position.presentation.controller.dto.PositionCreateReq
import com.hirelog.api.common.application.port.PagedResult
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/position")
@PreAuthorize("hasRole('ADMIN')")
class PositionController(
    private val positionWriteService: PositionWriteService,
    private val positionQuery: PositionQuery,
    private val positionCategoryCommand: PositionCategoryCommand
) {

    /**
     * Position 생성
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    fun create(
        @Valid @RequestBody request: PositionCreateReq
    ): ResponseEntity<PositionDetailView> {

        val category = positionCategoryCommand.findById(request.categoryId)
            ?: throw EntityNotFoundException("PositionCategory", request.categoryId)

        val position = positionWriteService.create(
            name = request.name,
            positionCategory = category,
            description = request.description
        )

        val view = positionQuery.findDetailById(position.id)
            ?: throw IllegalStateException("Position 생성 직후 조회 실패: ${position.id}")

        return ResponseEntity.status(HttpStatus.CREATED).body(view)
    }

    /**
     * Position 단건 조회
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{positionId}")
    fun getById(
        @PathVariable positionId: Long
    ): ResponseEntity<PositionDetailView> {

        val view = positionQuery.findDetailById(positionId)
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(view)
    }

    /**
     * Position 목록 조회 (페이지네이션)
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    fun getAll(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<PagedResult<PositionSummaryView>> {

        val result = positionQuery.findAllPaged(page, size)
        return ResponseEntity.ok(result)
    }

    /**
     * Position 활성화
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{positionId}/activate")
    fun activate(
        @PathVariable positionId: Long
    ): ResponseEntity<Void> {

        positionWriteService.activate(positionId)
        return ResponseEntity.ok().build()
    }

    /**
     * Position 비활성화 (Deprecated)
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{positionId}/deprecate")
    fun deprecate(
        @PathVariable positionId: Long
    ): ResponseEntity<Void> {

        positionWriteService.deprecate(positionId)
        return ResponseEntity.ok().build()
    }
}
