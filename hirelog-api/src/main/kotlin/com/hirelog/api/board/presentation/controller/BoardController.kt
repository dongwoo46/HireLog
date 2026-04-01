package com.hirelog.api.board.presentation.controller

import com.hirelog.api.board.application.BoardLikeReadService
import com.hirelog.api.board.application.BoardLikeWriteService
import com.hirelog.api.board.application.BoardReadService
import com.hirelog.api.board.application.BoardWriteService
import com.hirelog.api.board.domain.BoardSortType
import com.hirelog.api.board.domain.BoardType
import com.hirelog.api.board.presentation.controller.dto.request.BoardWriteReq
import com.hirelog.api.board.presentation.controller.dto.response.BoardRes
import com.hirelog.api.board.presentation.controller.dto.response.LikeRes
import com.hirelog.api.common.application.port.PagedResult
import com.hirelog.api.common.config.security.AuthenticatedMember
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/boards")
class BoardController(
    private val writeService: BoardWriteService,
    private val readService: BoardReadService,
    private val likeWriteService: BoardLikeWriteService,
    private val likeReadService: BoardLikeReadService
) {

    /** POST /api/boards */
    @PostMapping
    fun write(
        @AuthenticationPrincipal member: AuthenticatedMember?,
        @RequestBody @Valid request: BoardWriteReq
    ): ResponseEntity<Map<String, Long>> {
        val board = writeService.write(
            memberId = member?.memberId,
            boardType = request.boardType,
            title = request.title,
            content = request.content,
            anonymous = request.anonymous
        )
        return ResponseEntity.status(201).body(mapOf("id" to board.id))
    }

    /** GET /api/boards?boardType=FREE&page=0&size=20 */
    @GetMapping
    fun getList(
        @RequestParam boardType: BoardType?,
        @RequestParam(required = false) keyword: String?,
        @RequestParam(defaultValue = "LATEST") sortBy: BoardSortType,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @AuthenticationPrincipal member: AuthenticatedMember?
    ): ResponseEntity<PagedResult<BoardRes>> {
        val includeDeleted = member?.isAdmin() == true
        val result = readService.findAll(
            boardType = boardType,
            memberId = null,
            keyword = keyword,
            sortBy = sortBy,
            includeDeleted = includeDeleted,
            page = page,
            size = size
        ).map { BoardRes.from(it) }
        return ResponseEntity.ok(result)
    }

    /** GET /api/boards/{id} */
    @GetMapping("/{id}")
    fun getDetail(
        @PathVariable id: Long,
        @AuthenticationPrincipal member: AuthenticatedMember?
    ): ResponseEntity<BoardRes> {
        val view = readService.findById(id = id, viewerMemberId = member?.memberId ?: -1L)
        return ResponseEntity.ok(BoardRes.from(view))
    }

    /** PATCH /api/boards/{id} */
    @PatchMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    fun update(
        @PathVariable id: Long,
        @AuthenticationPrincipal member: AuthenticatedMember,
        @RequestBody @Valid request: BoardWriteReq
    ): ResponseEntity<Void> {
        writeService.update(
            boardId = id,
            requesterId = member.memberId,
            isAdmin = member.isAdmin(),
            title = request.title,
            content = request.content,
            anonymous = request.anonymous
        )
        return ResponseEntity.noContent().build()
    }

    /** DELETE /api/boards/{id} */
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    fun delete(
        @PathVariable id: Long,
        @AuthenticationPrincipal member: AuthenticatedMember
    ): ResponseEntity<Void> {
        writeService.delete(boardId = id, requesterId = member.memberId, isAdmin = member.isAdmin())
        return ResponseEntity.noContent().build()
    }

    /** POST /api/boards/{id}/like */
    @PostMapping("/{id}/like")
    @PreAuthorize("isAuthenticated()")
    fun like(
        @PathVariable id: Long,
        @AuthenticationPrincipal member: AuthenticatedMember
    ): ResponseEntity<LikeRes> {
        likeWriteService.like(boardId = id, memberId = member.memberId)
        val stat = likeReadService.getStat(boardId = id, memberId = member.memberId)
        return ResponseEntity.ok(LikeRes(likeCount = stat.likeCount, liked = stat.liked))
    }

    /** DELETE /api/boards/{id}/like */
    @DeleteMapping("/{id}/like")
    @PreAuthorize("isAuthenticated()")
    fun unlike(
        @PathVariable id: Long,
        @AuthenticationPrincipal member: AuthenticatedMember
    ): ResponseEntity<LikeRes> {
        likeWriteService.unlike(boardId = id, memberId = member.memberId)
        val stat = likeReadService.getStat(boardId = id, memberId = member.memberId)
        return ResponseEntity.ok(LikeRes(likeCount = stat.likeCount, liked = stat.liked))
    }

    /** GET /api/boards/{id}/like */
    @GetMapping("/{id}/like")
    fun getLike(
        @PathVariable id: Long,
        @AuthenticationPrincipal member: AuthenticatedMember?
    ): ResponseEntity<LikeRes> {
        val stat = likeReadService.getStat(boardId = id, memberId = member?.memberId ?: -1L)
        return ResponseEntity.ok(LikeRes(likeCount = stat.likeCount, liked = stat.liked))
    }
}
