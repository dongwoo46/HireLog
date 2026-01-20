package com.hirelog.api.job.application.snapshot.dto

/**
 * JobSnapshot 조회 결과 View
 *
 * 특징:
 * - 읽기 전용
 * - Entity와 분리
 * - JPA/OpenSearch 공통 표현
 */
data class JobSnapshotView(

    val id: Long,

    val brandId: Long,
    val companyId: Long?,

    val positionId: Long,

    val sourceType: String,
    val sourceUrl: String?,

    val rawText: String,
    val contentHash: String
)
