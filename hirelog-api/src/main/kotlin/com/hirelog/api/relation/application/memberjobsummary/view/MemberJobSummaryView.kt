package com.hirelog.api.relation.application.memberjobsummary.view

import com.hirelog.api.relation.domain.type.MemberJobSummarySaveType
import java.time.LocalDateTime

/**
 * MemberJobSummaryView
 *
 * 책임:
 * - 사용자가 저장한 JD 요약 관계를 표현하는 Read Model
 * - UI / API 응답 전용
 *
 * 주의:
 * - 도메인 로직 ❌
 * - 상태 변경 ❌
 * - 엔티티 참조 ❌
 */
data class MemberJobSummaryView(
    val memberId: Long,
    val jobSummaryId: Long,
    val saveType: MemberJobSummarySaveType,
    val memo: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)
