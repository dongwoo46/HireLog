package com.hirelog.api.userrequest.presentation.controller

import com.hirelog.api.common.config.security.AuthenticatedMember
import com.hirelog.api.common.config.security.CurrentUser
import com.hirelog.api.common.logging.log
import com.hirelog.api.member.domain.MemberRole
import com.hirelog.api.userrequest.application.UserRequestCommentWriteService
import com.hirelog.api.userrequest.application.UserRequestWriteService
import com.hirelog.api.userrequest.application.port.PagedResult
import com.hirelog.api.userrequest.application.port.UserRequestCommentQuery
import com.hirelog.api.userrequest.application.port.UserRequestQuery
import com.hirelog.api.userrequest.domain.UserRequestCommentWriterType
import com.hirelog.api.userrequest.domain.UserRequestStatus
import com.hirelog.api.userrequest.presentation.controller.dto.*
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

/**
 * UserRequest Controller
 *
 * 책임:
 * - 사용자 요청 관련 HTTP API 제공
 */
@RestController
@RequestMapping("/api/user-requests")
class UserRequestController(
    private val userRequestWriteService: UserRequestWriteService,
    private val userRequestQuery: UserRequestQuery,
    private val commentWriteService: UserRequestCommentWriteService,
    private val commentQuery: UserRequestCommentQuery
) {

    /**
     * 사용자 요청 생성
     *
     * POST /api/user-requests
     */
    @PostMapping
    fun createUserRequest(
        @CurrentUser member: AuthenticatedMember,
        @RequestBody @Valid request: UserRequestCreateReq
    ): ResponseEntity<UserRequestRes> {
        val userRequest = userRequestWriteService.create(
            memberId = member.memberId,
            requestType = request.requestType,
            content = request.content
        )
        return ResponseEntity.ok(UserRequestRes.from(userRequest))
    }

    /**
     * 내 요청 목록 조회
     *
     * GET /api/user-requests/my
     */
    @GetMapping("/my")
    fun getMyUserRequests(
        @AuthenticationPrincipal member: AuthenticatedMember
    ): ResponseEntity<List<UserRequestRes>> {
        val requests = userRequestQuery.findAllByMemberId(member.memberId)
            .map(UserRequestRes::from)
        return ResponseEntity.ok(requests)
    }

    /**
     * 전체 요청 목록 조회 (관리자)
     *
     * GET /api/user-requests/admin
     */
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    fun getAllUserRequests(
        @CurrentUser member: AuthenticatedMember,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<PagedResult<UserRequestRes>> {

        val result = userRequestQuery.findAllPaged(page, size)
        return ResponseEntity.ok(
            PagedResult(
                items = result.items.map(UserRequestRes::from),
                page = result.page,
                size = result.size,
                totalElements = result.totalElements,
                totalPages = result.totalPages,
                hasNext = result.hasNext
            )
        )
    }

    /**
     * 상태별 요청 목록 조회 (관리자)
     *
     * GET /api/user-requests/admin/status/{status}
     */
    @GetMapping("/admin/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    fun getUserRequestsByStatus(
        @AuthenticationPrincipal member: AuthenticatedMember,
        @PathVariable status: UserRequestStatus,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<PagedResult<UserRequestRes>> {

        val result = userRequestQuery.findAllByStatus(status, page, size)
        return ResponseEntity.ok(
            PagedResult(
                items = result.items.map(UserRequestRes::from),
                page = result.page,
                size = result.size,
                totalElements = result.totalElements,
                totalPages = result.totalPages,
                hasNext = result.hasNext
            )
        )
    }

    /**
     * 요청 상태 변경 (관리자)
     *
     * PATCH /api/user-requests/{userRequestId}/status
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{userRequestId}/status")
    fun updateUserRequestStatus(
        @AuthenticationPrincipal member: AuthenticatedMember,
        @PathVariable userRequestId: Long,
        @RequestBody @Valid request: UserRequestStatusUpdateReq
    ): ResponseEntity<UserRequestRes> {
        val userRequest = userRequestWriteService.updateStatus(
            userRequestId = userRequestId,
            status = request.status
        )
        return ResponseEntity.ok(UserRequestRes.from(userRequest))
    }

    /**
     * 댓글 작성
     *
     * POST /api/user-requests/{userRequestId}/comments
     */
    @PostMapping("/{userRequestId}/comments")
    fun createComment(
        @CurrentUser member: AuthenticatedMember,
        @PathVariable userRequestId: Long,
        @RequestBody @Valid request: UserRequestCommentCreateReq
    ): ResponseEntity<UserRequestCommentRes> {
        val writerType = if (member.role == MemberRole.ADMIN) {
            UserRequestCommentWriterType.ADMIN
        } else {
            UserRequestCommentWriterType.USER
        }

        val comment = commentWriteService.create(
            userRequestId = userRequestId,
            writerType = writerType,
            writerId = member.memberId,
            content = request.content
        )
        return ResponseEntity.ok(UserRequestCommentRes.from(comment))
    }

    /**
     * 댓글 목록 조회
     *
     * GET /api/user-requests/{userRequestId}/comments
     */
    @GetMapping("/{userRequestId}/comments")
    fun getComments(
        @PathVariable userRequestId: Long
    ): ResponseEntity<List<UserRequestCommentRes>> {
        val comments = commentQuery.findAllByUserRequestId(userRequestId)
            .map(UserRequestCommentRes::from)
        return ResponseEntity.ok(comments)
    }

}
