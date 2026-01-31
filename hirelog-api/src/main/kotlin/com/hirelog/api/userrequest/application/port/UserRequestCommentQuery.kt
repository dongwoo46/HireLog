package com.hirelog.api.userrequest.application.port

import com.hirelog.api.userrequest.domain.UserRequestComment

/**
 * UserRequestComment Query Port
 *
 * 책임:
 * - UserRequestComment 조회 유스케이스 정의
 * - 영속성 기술로부터 완전히 분리
 */
interface UserRequestCommentQuery {

    /**
     * 특정 UserRequest의 모든 댓글 조회
     */
    fun findAllByUserRequestId(userRequestId: Long): List<UserRequestComment>
}
