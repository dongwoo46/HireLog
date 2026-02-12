package com.hirelog.api.userrequest.application

import com.hirelog.api.common.logging.log
import com.hirelog.api.member.application.port.MemberQuery
import com.hirelog.api.member.domain.MemberRole
import com.hirelog.api.member.domain.MemberStatus
import com.hirelog.api.notification.application.NotificationWriteService
import com.hirelog.api.notification.domain.type.NotificationReferenceType
import com.hirelog.api.notification.domain.type.NotificationType
import com.hirelog.api.userrequest.application.port.UserRequestCommand
import com.hirelog.api.userrequest.application.port.UserRequestQuery
import com.hirelog.api.userrequest.domain.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * UserRequest Write Service
 *
 * 책임:
 * - UserRequest 생성/수정 유스케이스 실행
 * - 트랜잭션 경계 정의
 */
@Service
class UserRequestWriteService(
    private val command: UserRequestCommand,
    private val query: UserRequestQuery,
    private val memberQuery: MemberQuery,
    private val notificationWriteService: NotificationWriteService
) {

    /**
     * 사용자 요청 생성
     */
    @Transactional
    fun create(
        memberId: Long,
        requestType: UserRequestType,
        title: String,
        content: String
    ): UserRequest {

        require(memberQuery.existsByIdAndStatus(memberId, MemberStatus.ACTIVE)) {
            "존재하지 않는 사용자입니다. memberId=$memberId"
        }

        val userRequest = UserRequest.create(
            memberId = memberId,
            requestType = requestType,
            title = title,
            content = content
        )

        return command.save(userRequest)
    }

    /**
     * 댓글 작성
     */
    @Transactional
    fun addComment(
        memberId: Long,
        memberRole: MemberRole,
        userRequestId: Long,
        content: String
    ): Long {

        val userRequest = query.findById(userRequestId)
            ?: throw IllegalArgumentException("UserRequest not found: $userRequestId")

        val writerType = when (memberRole) {
            MemberRole.ADMIN -> UserRequestCommentWriterType.ADMIN
            else -> UserRequestCommentWriterType.USER
        }

        val comment = userRequest.addComment(
            writerType = writerType,
            writerId = memberId,
            content = content
        )

        command.save(userRequest)

        // 요청자에게 답변 알림 (본인 댓글은 제외)
        if (userRequest.memberId != memberId) {
            try {
                notificationWriteService.create(
                    memberId = userRequest.memberId,
                    type = NotificationType.USER_REQUEST_REPLIED,
                    title = "내 요청에 답변이 등록되었습니다",
                    message = userRequest.title,
                    referenceType = NotificationReferenceType.USER_REQUEST,
                    referenceId = userRequest.id
                )
            } catch (e: Exception) {
                log.error(
                    "[NOTIFICATION_CREATE_FAILED] userRequestId={}, memberId={}, error={}",
                    userRequestId, userRequest.memberId, e.message, e
                )
            }
        }

        return comment.id
    }

    /**
     * 상태 변경 (관리자)
     */
    @Transactional
    fun updateStatus(
        memberRole: MemberRole,
        userRequestId: Long,
        status: UserRequestStatus
    ): UserRequest {

        require(memberRole == MemberRole.ADMIN) {
            "Only ADMIN can update user request status"
        }

        val userRequest = query.findById(userRequestId)
            ?: throw IllegalArgumentException("UserRequest not found: $userRequestId")

        userRequest.updateStatus(status)
        return command.save(userRequest)
    }
}

