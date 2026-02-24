package com.hirelog.api.userrequest.presentation.controller

import com.hirelog.api.common.config.security.AuthenticatedMember
import com.hirelog.api.common.config.security.CurrentUser
import com.hirelog.api.userrequest.application.UserRequestReadService
import com.hirelog.api.userrequest.application.UserRequestWriteService
import com.hirelog.api.common.application.port.PagedResult
import com.hirelog.api.common.logging.log
import com.hirelog.api.userrequest.domain.UserRequestStatus
import com.hirelog.api.userrequest.presentation.controller.dto.*
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/user-requests")
class UserRequestController(
    private val userRequestWriteService: UserRequestWriteService,
    private val userRequestReadService: UserRequestReadService
) {

    /**
     * 사용자 요청 생성
     */
    @PostMapping
    fun createUserRequest(
        @CurrentUser member: AuthenticatedMember,
        @RequestBody @Valid request: UserRequestCreateReq
    ): ResponseEntity<Void> {

        val userRequest = userRequestWriteService.create(
            memberId = member.memberId,
            requestType = request.requestType,
            title = request.title,
            content = request.content
        )

        return ResponseEntity.status(HttpStatus.CREATED).build()
    }

    /**
     * 내 요청 목록 조회
     */
    @GetMapping("/my")
    fun getMyUserRequests(
        @CurrentUser member: AuthenticatedMember,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<PagedResult<UserRequestListRes>> {

        val result = userRequestReadService
            .getMyRequests(member.memberId, page, size)
            .map(UserRequestListRes::from)

        return ResponseEntity.ok(result)
    }

    /**
     * 요청 상세 조회 (댓글 포함)
     *
     * - ADMIN: 모든 요청
     * - USER : 본인 요청
     */
    @GetMapping("/{userRequestId}")
    fun getUserRequestDetail(
        @CurrentUser member: AuthenticatedMember,
        @PathVariable userRequestId: Long
    ): ResponseEntity<UserRequestDetailRes> {

        val userRequest = userRequestReadService.getRequestDetail(
            memberId = member.memberId,
            memberRole = member.role,
            userRequestId = userRequestId
        )

        return ResponseEntity.ok(
            UserRequestDetailRes.from(userRequest)
        )
    }

    /**
     * 전체 요청 목록 조회 (관리자)
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin")
    fun getAllUserRequests(
        @RequestParam(required = false) status: UserRequestStatus?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @CurrentUser member: AuthenticatedMember
    ): ResponseEntity<PagedResult<UserRequestListRes>> {
        log.info("[USER REQUEST] member role: {}, memberId: {}", member.role,member.memberId)
        val result = userRequestReadService.getPaged(
            status = status,
            page = page,
            size = size
        )

        return ResponseEntity.ok(
            result.map(UserRequestListRes::from)
        )
    }

    /**
     * 요청 상태 변경 (관리자)
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{userRequestId}/status")
    fun updateUserRequestStatus(
        @CurrentUser member: AuthenticatedMember,
        @PathVariable userRequestId: Long,
        @RequestBody @Valid request: UserRequestStatusUpdateReq,
    ): ResponseEntity<UserRequestRes> {
        log.info("[USER REQUEST] member role: {}, memberId: {}", member.role,member.memberId)

        val userRequest = userRequestWriteService.updateStatus(
            memberRole = member.role,
            userRequestId = userRequestId,
            status = request.status
        )

        return ResponseEntity.ok(UserRequestRes.from(userRequest))
    }

    /**
     * 댓글 작성
     */
    @PostMapping("/{userRequestId}/comments")
    fun addComment(
        @CurrentUser member: AuthenticatedMember,
        @PathVariable userRequestId: Long,
        @RequestBody @Valid request: UserRequestCommentCreateReq
    ): ResponseEntity<Void> {

        userRequestWriteService.addComment(
            memberId = member.memberId,
            memberRole = member.role,
            userRequestId = userRequestId,
            content = request.content
        )

        return ResponseEntity.status(HttpStatus.CREATED).build()
    }
}
