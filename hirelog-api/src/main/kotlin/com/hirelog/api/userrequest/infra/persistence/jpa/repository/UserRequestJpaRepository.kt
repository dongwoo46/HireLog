package com.hirelog.api.userrequest.infra.persistence.jpa.repository

import com.hirelog.api.userrequest.domain.UserRequest
import com.hirelog.api.userrequest.domain.UserRequestStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

/**
 * UserRequest JPA Repository
 *
 * 책임:
 * - UserRequest Entity CRUD
 */
interface UserRequestJpaRepository : JpaRepository<UserRequest, Long> {

    fun findAllByMemberIdOrderByIdDesc(memberId: Long): List<UserRequest>

    fun findAllByStatusOrderByIdDesc(status: UserRequestStatus, pageable: Pageable): Page<UserRequest>

    fun findAllByOrderByIdDesc(pageable: Pageable): Page<UserRequest>
}
