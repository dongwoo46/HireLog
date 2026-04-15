package com.hirelog.api.job.application.rag.model

import com.hirelog.api.job.domain.type.HiringStage
import com.hirelog.api.relation.domain.type.HiringStageResult
import com.hirelog.api.relation.domain.type.MemberJobSummarySaveType

/**
 * RAG 쿼리 필터
 *
 * cohortFilters: DB cohort 조회용 (PATTERN_ANALYSIS, EXPERIENCE_ANALYSIS)
 * searchFilters: OpenSearch 필터용 (k-NN + aggregation)
 */
data class RagFilters(

    // === cohort 필터 (DB 조회) ===
    val saveType: MemberJobSummarySaveType? = null,
    val stage: HiringStage? = null,
    val stageResult: HiringStageResult? = null,

    // === OpenSearch 필터 ===
    val careerType: String? = null,
    val companyDomain: String? = null,
    val techStacks: List<String>? = null,
    val brandName: String? = null,
    val dateRangeFrom: String? = null,   // ISO-8601
    val dateRangeTo: String? = null      // ISO-8601
)
