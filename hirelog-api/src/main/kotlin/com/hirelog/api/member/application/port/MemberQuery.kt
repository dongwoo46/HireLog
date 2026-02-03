package com.hirelog.api.member.application.port

import com.hirelog.api.member.application.view.MemberDetailView
import com.hirelog.api.member.application.view.MemberSummaryView
import com.hirelog.api.member.domain.MemberStatus
import com.hirelog.api.userrequest.application.port.PagedResult

/**
 * Member Query Port
 *
 * 책임:
 * - Member 조회 계약 정의 (View 반환)
 * - exists 검증
 */
interface MemberQuery {

    fun findAllPaged(page: Int, size: Int): PagedResult<MemberSummaryView>

    fun findDetailById(id: Long): MemberDetailView?

    fun existsById(id: Long): Boolean

    fun existsByIdAndStatus(id: Long, status: MemberStatus): Boolean

    fun existsByUsername(username: String): Boolean

    fun existsByEmail(email: String): Boolean
}
