package com.hirelog.api.userrequest.application

import com.hirelog.api.userrequest.application.port.UserRequestCommand
import com.hirelog.api.userrequest.application.port.UserRequestQuery
import com.hirelog.api.userrequest.domain.UserRequest
import com.hirelog.api.userrequest.domain.UserRequestStatus
import com.hirelog.api.userrequest.domain.UserRequestType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

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
    private val query: UserRequestQuery
) {

    /**
     * 사용자 요청 생성
     */
    @Transactional
    fun create(
        memberId: Long,
        requestType: UserRequestType,
        content: String
    ): UserRequest {
        val userRequest = UserRequest(
            memberId = memberId,
            requestType = requestType,
            content = content
        )
        return command.save(userRequest)
    }

    /**
     * 상태 변경 (관리자)
     */
    @Transactional
    fun updateStatus(
        userRequestId: Long,
        status: UserRequestStatus
    ): UserRequest {
        val userRequest = query.findById(userRequestId)
            ?: throw IllegalArgumentException("UserRequest not found: $userRequestId")

        userRequest.status = status

        if (status == UserRequestStatus.RESOLVED || status == UserRequestStatus.REJECTED) {
            userRequest.resolvedAt = LocalDateTime.now()
        }

        return command.save(userRequest)
    }
}
