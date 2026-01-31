package com.hirelog.api.userrequest.application.port

import com.hirelog.api.userrequest.domain.UserRequest
import com.hirelog.api.userrequest.domain.UserRequestStatus

/**
 * UserRequest Query Port
 *
 * 책임:
 * - UserRequest 조회 유스케이스 정의
 * - 영속성 기술로부터 완전히 분리
 */
interface UserRequestQuery {

    /**
     * ID로 UserRequest 조회
     */
    fun findById(id: Long): UserRequest?

    /**
     * 특정 회원의 모든 요청 조회
     */
    fun findAllByMemberId(memberId: Long): List<UserRequest>

    /**
     * 특정 상태의 요청 페이징 조회
     */
    fun findAllByStatus(
        status: UserRequestStatus,
        page: Int,
        size: Int
    ): PagedResult<UserRequest>

    /**
     * 전체 요청 페이징 조회 (관리자용)
     */
    fun findAllPaged(
        page: Int,
        size: Int
    ): PagedResult<UserRequest>
}

/**
 * 페이징 결과
 */
data class PagedResult<T>(
    val items: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val hasNext: Boolean
)
