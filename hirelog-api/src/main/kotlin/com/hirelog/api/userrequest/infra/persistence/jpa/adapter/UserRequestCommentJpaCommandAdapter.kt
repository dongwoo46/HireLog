package com.hirelog.api.userrequest.infra.persistence.jpa.adapter

import com.hirelog.api.userrequest.application.port.UserRequestCommentCommand
import com.hirelog.api.userrequest.domain.UserRequestComment
import com.hirelog.api.userrequest.infra.persistence.jpa.repository.UserRequestCommentJpaRepository
import org.springframework.stereotype.Component

/**
 * UserRequestComment JPA Command Adapter
 *
 * 책임:
 * - UserRequestCommentCommand Port를 JPA로 구현
 */
@Component
class UserRequestCommentJpaCommandAdapter(
    private val repository: UserRequestCommentJpaRepository
) : UserRequestCommentCommand {

    override fun save(comment: UserRequestComment): UserRequestComment {
        return repository.save(comment)
    }
}
