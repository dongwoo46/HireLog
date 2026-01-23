package com.hirelog.api.job.application.snapshot.view

import com.hirelog.api.job.domain.JobSourceType

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

    /**
     * Snapshot 식별자
     */
    val id: Long,

    /**
     * 분석 완료 후 연결되는 브랜드 ID
     *
     * - 분석 전에는 null
     */
    val brandId: Long?,

    /**
     * 분석 완료 후 연결되는 포지션 ID
     *
     * - 분석 전에는 null
     */
    val positionId: Long?,

    /**
     * JD 수집 소스 타입
     */
    val sourceType: JobSourceType,

    /**
     * JD 원본 URL
     *
     * - OCR / 수동 입력 등에서는 null
     */
    val sourceUrl: String?,

    /**
     * JD 원문 텍스트
     *
     * - 최초 수집 원본 그대로
     */
    val rawText: String,

    /**
     * JD 내용 기반 해시
     *
     * 용도:
     * - 운영 추적
     * - 중복 분석 보조
     */
    val contentHash: String
)
