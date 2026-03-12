package com.hirelog.api.userrequest.application

import com.hirelog.api.member.domain.MemberRole
import com.hirelog.api.userrequest.application.port.UserRequestQuery
import com.hirelog.api.userrequest.domain.UserRequest
import com.hirelog.api.userrequest.domain.UserRequestStatus
import org.springframework.stereotype.Service

@Service
class UserRequestReadService(
    private val userRequestQuery: UserRequestQuery
) {

    /**
     * 내 요청 목록 조회 (페이징)
     */
    fun getMyRequests(
        memberId: Long,
        page: Int,
        size: Int
    ) = userRequestQuery.findByMemberId(memberId, page, size)

    /**
     * 요청 상세 조회 (댓글 포함)
     *
     * 접근 정책:
     * - ADMIN: 모든 요청
     * - USER : 본인 요청만
     */
    fun getRequestDetail(
        memberId: Long,
        memberRole: MemberRole,
        userRequestId: Long
    ): UserRequest {

        val request = userRequestQuery.findDetailById(userRequestId)
            ?: throw IllegalArgumentException("UserRequest not found: $userRequestId")

        if (memberRole != MemberRole.ADMIN &&
            request.memberId != memberId
        ) {
            throw IllegalStateException("Access denied")
        }

        return request
    }

    /**
     * 관리자 전체 요청 조회
     */
    fun getPaged(
        status: UserRequestStatus?,
        page: Int,
        size: Int
    ) = userRequestQuery.findPaged(status, page, size)
}
