package com.hirelog.api.job.application.snapshot.view

import com.hirelog.api.job.domain.JobSourceType
import com.hirelog.api.job.domain.RecruitmentPeriodType
import java.time.LocalDate

/**
 * JobSnapshot 조회 결과 View
 *
 * 역할:
 * - JD 수집 시점의 Snapshot 상태 표현
 * - 분석 완료 여부를 포함한 읽기 전용 모델
 *
 * 특징:
 * - Entity와 분리
 * - brand / position 미연결 상태 허용
 * - JPA / OpenSearch 공통 사용 가능
 */
data class JobSnapshotView(

    val id: Long,

    val brandId: Long?,
    val positionId: Long?,

    val sourceType: JobSourceType,
    val sourceUrl: String?,

    /**
     * 전처리된 JD 구조
     *
     * - responsibilities
     * - requirements
     * - preferred
     * - techStack
     */
    val canonicalSections: Map<String, List<String>>,

    val recruitmentPeriodType: RecruitmentPeriodType,
    val openedDate: LocalDate?,
    val closedDate: LocalDate?,

    /**
     * Fast-path 중복 판정용
     */
//    val contentHash: String,
)
