package com.hirelog.api.userrequest.application.port

import com.hirelog.api.common.application.port.PagedResult
import com.hirelog.api.userrequest.application.view.UserRequestView
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
     * 단건 조회 (연관 로딩 없음)
     */
    fun findById(id: Long): UserRequest?

    /**
     * 상세 조회 (댓글 포함)
     */
    fun findDetailById(id: Long): UserRequest?

    /**
     * 특정 회원의 요청 목록
     */
    fun findAllByMemberId(memberId: Long): List<UserRequestView>

    /**
     * 관리자 / 상태별 통합 페이징 조회
     *
     * status == null → 전체 조회
     */
    fun findPaged(
        status: UserRequestStatus? = null,
        page: Int,
        size: Int
    ): PagedResult<UserRequestView>
}

