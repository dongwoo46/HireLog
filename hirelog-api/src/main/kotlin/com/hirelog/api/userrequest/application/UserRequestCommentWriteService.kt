package com.hirelog.api.userrequest.application

import com.hirelog.api.userrequest.application.port.UserRequestCommentCommand
import com.hirelog.api.userrequest.application.port.UserRequestQuery
import com.hirelog.api.userrequest.domain.UserRequestComment
import com.hirelog.api.userrequest.domain.UserRequestCommentWriterType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * UserRequestComment Write Service
 *
 * 책임:
 * - UserRequestComment 생성 유스케이스 실행
 * - 트랜잭션 경계 정의
 */
@Service
class UserRequestCommentWriteService(
    private val command: UserRequestCommentCommand,
    private val userRequestQuery: UserRequestQuery
) {

    /**
     * 댓글 작성
     */
    @Transactional
    fun create(
        userRequestId: Long,
        writerType: UserRequestCommentWriterType,
        writerId: Long,
        content: String
    ): UserRequestComment {
        val userRequest = userRequestQuery.findById(userRequestId)
            ?: throw IllegalArgumentException("UserRequest not found: $userRequestId")

        val comment = UserRequestComment.create(
            userRequest = userRequest,
            writerType = writerType,
            writerId = writerId,
            content = content
        )

        return command.save(comment)
    }
}
