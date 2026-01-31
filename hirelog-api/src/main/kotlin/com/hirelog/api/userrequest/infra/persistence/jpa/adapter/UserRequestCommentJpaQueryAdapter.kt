package com.hirelog.api.userrequest.infra.persistence.jpa.adapter

import com.hirelog.api.userrequest.application.port.UserRequestCommentQuery
import com.hirelog.api.userrequest.domain.UserRequestComment
import com.hirelog.api.userrequest.infra.persistence.jpa.repository.UserRequestCommentJpaRepository
import org.springframework.stereotype.Component

/**
 * UserRequestComment JPA Query Adapter
 *
 * 책임:
 * - UserRequestCommentQuery Port를 JPA로 구현
 */
@Component
class UserRequestCommentJpaQueryAdapter(
    private val repository: UserRequestCommentJpaRepository
) : UserRequestCommentQuery {

    override fun findAllByUserRequestId(userRequestId: Long): List<UserRequestComment> {
        return repository.findAllByUserRequestIdOrderByIdAsc(userRequestId)
    }
}
