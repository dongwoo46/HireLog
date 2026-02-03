package com.hirelog.api.member.application.view

import com.hirelog.api.member.domain.MemberRole
import com.hirelog.api.member.domain.MemberStatus
import java.time.LocalDateTime

/**
 * MemberDetailView
 *
 * 회원 상세 조회용 View
 */
data class MemberDetailView(
    val id: Long,
    val email: String,
    val username: String,
    val role: MemberRole,
    val status: MemberStatus,
    val currentPositionId: Long?,
    val careerYears: Int?,
    val summary: String?,
    val createdAt: LocalDateTime
)
