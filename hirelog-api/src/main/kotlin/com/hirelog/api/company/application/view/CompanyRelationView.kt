package com.hirelog.api.company.application.view

import com.hirelog.api.company.domain.CompanyRelationType
import java.time.LocalDateTime

/**
 * CompanyRelationView
 *
 * 책임:
 * - 회사 관계 조회/관리용 View
 */
data class CompanyRelationView(
    val id: Long,
    val parentCompanyId: Long,
    val childCompanyId: Long,
    val relationType: CompanyRelationType,
    val createdAt: LocalDateTime
)
