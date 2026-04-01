package com.hirelog.api.comment.presentation.controller

import com.hirelog.api.board.presentation.controller.dto.response.LikeRes
import com.hirelog.api.comment.application.CommentLikeReadService
import com.hirelog.api.comment.application.CommentLikeWriteService
import com.hirelog.api.comment.application.CommentReadService
import com.hirelog.api.comment.application.CommentWriteService
import com.hirelog.api.comment.presentation.controller.dto.request.CommentWriteReq
import com.hirelog.api.comment.presentation.controller.dto.response.CommentRes
import com.hirelog.api.common.application.port.PagedResult
import com.hirelog.api.common.config.security.AuthenticatedMember
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/boards/{boardId}/comments")
class CommentController(
    private val writeService: CommentWriteService,
    private val readService: CommentReadService,
    private val likeWriteService: CommentLikeWriteService,
    private val likeReadService: CommentLikeReadService
) {

    /** POST /api/boards/{boardId}/comments */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    fun write(
        @PathVariable boardId: Long,
        @AuthenticationPrincipal member: AuthenticatedMember,
        @RequestBody @Valid request: CommentWriteReq
    ): ResponseEntity<Map<String, Long>> {
        val comment = writeService.write(
            boardId = boardId,
            memberId = member.memberId,
            content = request.content,
            anonymous = request.anonymous
        )
        return ResponseEntity.status(201).body(mapOf("id" to comment.id))
    }

    /** GET /api/boards/{boardId}/comments?page=0&size=20 */
    @GetMapping
    fun getList(
        @PathVariable boardId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @AuthenticationPrincipal member: AuthenticatedMember?
    ): ResponseEntity<PagedResult<CommentRes>> {
        val includeDeleted = member?.isAdmin() == true
        val result = readService.findByBoardId(
            boardId = boardId,
            includeDeleted = includeDeleted,
            page = page,
            size = size
        ).map { CommentRes.from(it) }
        return ResponseEntity.ok(result)
    }

    /** PATCH /api/boards/{boardId}/comments/{id} */
    @PatchMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    fun update(
        @PathVariable boardId: Long,
        @PathVariable id: Long,
        @AuthenticationPrincipal member: AuthenticatedMember,
        @RequestBody @Valid request: CommentWriteReq
    ): ResponseEntity<Void> {
        writeService.update(
            commentId = id,
            requesterId = member.memberId,
            isAdmin = member.isAdmin(),
            content = request.content,
            anonymous = request.anonymous
        )
        return ResponseEntity.noContent().build()
    }

    /** DELETE /api/boards/{boardId}/comments/{id} */
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    fun delete(
        @PathVariable boardId: Long,
        @PathVariable id: Long,
        @AuthenticationPrincipal member: AuthenticatedMember
    ): ResponseEntity<Void> {
        writeService.delete(commentId = id, requesterId = member.memberId, isAdmin = member.isAdmin())
        return ResponseEntity.noContent().build()
    }

    /** POST /api/boards/{boardId}/comments/{id}/like */
    @PostMapping("/{id}/like")
    @PreAuthorize("isAuthenticated()")
    fun like(
        @PathVariable boardId: Long,
        @PathVariable id: Long,
        @AuthenticationPrincipal member: AuthenticatedMember
    ): ResponseEntity<LikeRes> {
        likeWriteService.like(commentId = id, memberId = member.memberId)
        val stat = likeReadService.getStat(commentId = id, memberId = member.memberId)
        return ResponseEntity.ok(LikeRes(likeCount = stat.likeCount, liked = stat.liked))
    }

    /** DELETE /api/boards/{boardId}/comments/{id}/like */
    @DeleteMapping("/{id}/like")
    @PreAuthorize("isAuthenticated()")
    fun unlike(
        @PathVariable boardId: Long,
        @PathVariable id: Long,
        @AuthenticationPrincipal member: AuthenticatedMember
    ): ResponseEntity<LikeRes> {
        likeWriteService.unlike(commentId = id, memberId = member.memberId)
        val stat = likeReadService.getStat(commentId = id, memberId = member.memberId)
        return ResponseEntity.ok(LikeRes(likeCount = stat.likeCount, liked = stat.liked))
    }
}
