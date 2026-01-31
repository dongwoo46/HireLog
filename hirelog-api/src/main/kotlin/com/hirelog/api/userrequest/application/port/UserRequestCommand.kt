package com.hirelog.api.userrequest.application.port

import com.hirelog.api.userrequest.domain.UserRequest

/**
 * UserRequest Command Port
 *
 * 책임:
 * - UserRequest 영속화
 * - 저장소 기술로부터 완전히 분리
 */
interface UserRequestCommand {

    /**
     * UserRequest 저장
     */
    fun save(userRequest: UserRequest): UserRequest
}
