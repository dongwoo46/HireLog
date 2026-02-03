package com.hirelog.api.member.infra.persistence.jpa.adapter

import com.hirelog.api.member.application.port.MemberQuery
import com.hirelog.api.member.application.view.MemberDetailView
import com.hirelog.api.member.application.view.MemberSummaryView
import com.hirelog.api.member.domain.MemberStatus
import com.hirelog.api.member.infra.persistence.jpa.repository.MemberJpaQueryDsl
import com.hirelog.api.userrequest.application.port.PagedResult
import org.springframework.stereotype.Component

/**
 * Member JPA Query Adapter
 *
 * 책임:
 * - MemberQuery Port 구현
 * - QueryDSL 구현체로 위임
 */
@Component
class MemberJpaQuery(
    private val queryDsl: MemberJpaQueryDsl
) : MemberQuery {

    override fun findAllPaged(page: Int, size: Int): PagedResult<MemberSummaryView> =
        queryDsl.findAllPaged(page, size)

    override fun findDetailById(id: Long): MemberDetailView? =
        queryDsl.findDetailById(id)

    override fun existsById(id: Long): Boolean =
        queryDsl.existsById(id)

    override fun existsByIdAndStatus(id: Long, status: MemberStatus): Boolean =
        queryDsl.existsByIdAndStatus(id, status)

    override fun existsByUsername(username: String): Boolean =
        queryDsl.existsByUsername(username)

    override fun existsByEmail(email: String): Boolean =
        queryDsl.existsByEmail(email)
}
