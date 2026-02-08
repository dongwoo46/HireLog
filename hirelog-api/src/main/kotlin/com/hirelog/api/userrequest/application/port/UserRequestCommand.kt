package com.hirelog.api.userrequest.application.port

import com.hirelog.api.userrequest.domain.UserRequest

/**
 * UserRequest Command Port
 *
 * 책임:
 * - UserRequest Aggregate 영속화
 * - Aggregate 내부 변경 사항을 저장
 *
 * 제약:
 * - Comment 등 하위 엔티티는 직접 저장하지 않는다
 */
interface UserRequestCommand {

    fun save(userRequest: UserRequest): UserRequest
}

