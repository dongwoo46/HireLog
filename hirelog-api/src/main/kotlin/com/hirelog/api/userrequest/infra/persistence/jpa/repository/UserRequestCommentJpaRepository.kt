package com.hirelog.api.userrequest.infra.persistence.jpa.repository

import com.hirelog.api.userrequest.domain.UserRequestComment
import org.springframework.data.jpa.repository.JpaRepository

/**
 * UserRequestComment JPA Repository
 *
 * 책임:
 * - UserRequestComment Entity CRUD
 */
interface UserRequestCommentJpaRepository : JpaRepository<UserRequestComment, Long> {

    fun findAllByUserRequestIdOrderByIdAsc(userRequestId: Long): List<UserRequestComment>
}
