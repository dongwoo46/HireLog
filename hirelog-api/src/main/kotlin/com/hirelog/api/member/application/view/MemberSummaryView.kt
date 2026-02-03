package com.hirelog.api.member.application.view

import com.hirelog.api.member.domain.MemberRole
import com.hirelog.api.member.domain.MemberStatus

/**
 * MemberSummaryView
 *
 * 회원 목록 조회용 최소 정보
 */
data class MemberSummaryView(
    val id: Long,
    val email: String,
    val username: String,
    val role: MemberRole,
    val status: MemberStatus
)
