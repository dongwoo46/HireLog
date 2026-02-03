package com.hirelog.api.userrequest.application

import com.hirelog.api.member.application.port.MemberQuery
import com.hirelog.api.member.domain.MemberStatus
import com.hirelog.api.userrequest.application.port.UserRequestCommand
import com.hirelog.api.userrequest.application.port.UserRequestQuery
import com.hirelog.api.userrequest.domain.UserRequest
import com.hirelog.api.userrequest.domain.UserRequestStatus
import com.hirelog.api.userrequest.domain.UserRequestType
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
    private val memberQuery: MemberQuery
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

        // 1. 사용자 존재 여부 검증
        require(memberQuery.existsByIdAndStatus(memberId, MemberStatus.ACTIVE)) {
            "존재하지 않는 사용자입니다. memberId=$memberId"
        }

        val userRequest = UserRequest.create(
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

        userRequest.updateStatus(status)

        return command.save(userRequest)
    }
}
