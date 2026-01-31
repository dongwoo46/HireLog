package com.hirelog.api.userrequest.infra.persistence.jpa.adapter

import com.hirelog.api.userrequest.application.port.UserRequestCommand
import com.hirelog.api.userrequest.domain.UserRequest
import com.hirelog.api.userrequest.infra.persistence.jpa.repository.UserRequestJpaRepository
import org.springframework.stereotype.Component

/**
 * UserRequest JPA Command Adapter
 *
 * 책임:
 * - UserRequestCommand Port를 JPA로 구현
 */
@Component
class UserRequestJpaCommandAdapter(
    private val repository: UserRequestJpaRepository
) : UserRequestCommand {

    override fun save(userRequest: UserRequest): UserRequest {
        return repository.save(userRequest)
    }
}
