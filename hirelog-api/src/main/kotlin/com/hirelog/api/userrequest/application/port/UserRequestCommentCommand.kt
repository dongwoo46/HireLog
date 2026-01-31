package com.hirelog.api.userrequest.application.port

import com.hirelog.api.userrequest.domain.UserRequestComment

/**
 * UserRequestComment Command Port
 *
 * 책임:
 * - UserRequestComment 영속화
 * - 저장소 기술로부터 완전히 분리
 */
interface UserRequestCommentCommand {

    /**
     * UserRequestComment 저장
     */
    fun save(comment: UserRequestComment): UserRequestComment
}
