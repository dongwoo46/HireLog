package com.hirelog.api.job.application.snapshot.port

import com.hirelog.api.job.domain.model.JobSnapshot
import java.time.LocalDate

/**
 * JobSnapshot Command Port
 *
 * 책임:
 * - JobSnapshot 생성 / 상태 변경
 * - Entity 로딩 (상태 변경 / 중복 판정 등)
 *
 * 설계 원칙:
 * - Entity를 반환하는 모든 메서드는 Command Port에 위치
 * - Query Port는 View(Read Model)만 반환
 */
interface JobSnapshotCommand {

    /**
     * Snapshot 최초 기록
     *
     * 사용 시점:
     * - JD 수집 직후
     */
    fun record(
        snapshot: JobSnapshot
    ): Long

    /**
     * 분석 결과 반영
     *
     * 규칙:
     * - brandId / positionId는 단 한 번만 설정됨
     */
    fun update(
        snapshot: JobSnapshot
    )

    /**
     * Entity 단건 로딩
     */
    fun findById(id: Long): JobSnapshot?

    /**
     * URL 기준 Snapshot Entity 목록 로딩
     *
     * 사용 목적:
     * - 중복 판정 의심 후보 수집
     */
    fun findAllBySourceUrl(url: String): List<JobSnapshot>

    /**
     * 기간 기준 Snapshot Entity 목록 로딩
     *
     * 사용 목적:
     * - 중복 판정 의심 후보 수집
     */
    fun findAllByDateRange(
        openedDate: LocalDate?,
        closedDate: LocalDate?
    ): List<JobSnapshot>

    /**
     * pg_trgm 기반 유사 Snapshot Entity 목록 로딩
     *
     * 사용 목적:
     * - 텍스트 유사도 기반 중복 판정
     */
    fun findSimilarByCoreText(
        coreText: String,
        threshold: Double
    ): List<JobSnapshot>
}
