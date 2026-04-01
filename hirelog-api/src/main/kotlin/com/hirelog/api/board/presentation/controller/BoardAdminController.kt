package com.hirelog.api.board.presentation.controller

import com.hirelog.api.board.application.BoardReadService
import com.hirelog.api.board.domain.BoardSortType
import com.hirelog.api.board.domain.BoardType
import com.hirelog.api.board.presentation.controller.dto.response.BoardRes
import com.hirelog.api.common.application.port.PagedResult
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/boards")
@PreAuthorize("hasRole('ADMIN')")
class BoardAdminController(
    private val readService: BoardReadService
) {

    /** GET /api/admin/boards */
    @GetMapping
    fun getList(
        @RequestParam boardType: BoardType?,
        @RequestParam(required = false) keyword: String?,
        @RequestParam(defaultValue = "LATEST") sortBy: BoardSortType,
        @RequestParam(required = false) deleted: Boolean?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<PagedResult<BoardRes>> {
        val result = readService.findAll(
            boardType = boardType,
            memberId = null,
            keyword = keyword,
            sortBy = sortBy,
            deleted = deleted,
            includeDeleted = true,
            page = page,
            size = size
        ).map { BoardRes.from(it) }
        return ResponseEntity.ok(result)
    }
}

