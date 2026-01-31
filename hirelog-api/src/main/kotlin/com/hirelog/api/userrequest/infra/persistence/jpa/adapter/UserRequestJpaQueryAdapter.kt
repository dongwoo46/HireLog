package com.hirelog.api.userrequest.infra.persistence.jpa.adapter

import com.hirelog.api.userrequest.application.port.PagedResult
import com.hirelog.api.userrequest.application.port.UserRequestQuery
import com.hirelog.api.userrequest.domain.UserRequest
import com.hirelog.api.userrequest.domain.UserRequestStatus
import com.hirelog.api.userrequest.infra.persistence.jpa.repository.UserRequestJpaRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

/**
 * UserRequest JPA Query Adapter
 *
 * 책임:
 * - UserRequestQuery Port를 JPA로 구현
 */
@Component
class UserRequestJpaQueryAdapter(
    private val repository: UserRequestJpaRepository
) : UserRequestQuery {

    override fun findById(id: Long): UserRequest? {
        return repository.findByIdOrNull(id)
    }

    override fun findAllByMemberId(memberId: Long): List<UserRequest> {
        return repository.findAllByMemberIdOrderByIdDesc(memberId)
    }

    override fun findAllByStatus(
        status: UserRequestStatus,
        page: Int,
        size: Int
    ): PagedResult<UserRequest> {
        require(page >= 0) { "page는 0 이상이어야 합니다." }
        require(size in 1..100) { "size는 1~100 사이여야 합니다." }

        val pageable = PageRequest.of(page, size)
        val result = repository.findAllByStatusOrderByIdDesc(status, pageable)

        return PagedResult(
            items = result.content,
            page = result.number,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
            hasNext = result.hasNext()
        )
    }

    override fun findAllPaged(
        page: Int,
        size: Int
    ): PagedResult<UserRequest> {
        require(page >= 0) { "page는 0 이상이어야 합니다." }
        require(size in 1..100) { "size는 1~100 사이여야 합니다." }

        val pageable = PageRequest.of(page, size)
        val result = repository.findAllByOrderByIdDesc(pageable)

        return PagedResult(
            items = result.content,
            page = result.number,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
            hasNext = result.hasNext()
        )
    }
}
