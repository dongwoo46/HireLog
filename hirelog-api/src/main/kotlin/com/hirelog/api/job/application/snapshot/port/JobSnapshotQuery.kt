package com.hirelog.api.job.application.snapshot.port

import com.hirelog.api.job.application.snapshot.view.JobSnapshotView
import com.hirelog.api.job.domain.JobSnapshot
import java.time.LocalDate

/**
 * JobSnapshot 조회 Port
 *
 * 책임:
 * - JobSnapshot 조회 유스케이스 정의
 *
 * 설계 의도:
 * - 기본적으로 Query 결과는 Read Model(View)을 반환한다
 * - 단, Snapshot의 "상태 변경(Command 흐름)"을 위해
 *   제한적으로 Entity 로딩 메서드를 허용한다
 *
 * 사용 규칙:
 * - get list* : 조회/화면/중복 판정용 (View 반환)
 * - load*      : 상태 변경을 위한 Command 준비 단계에서만 사용 (Entity 반환)
 */
interface JobSnapshotQuery {

    /* ===================== */
    /* Read Model(View) 조회 */
    /* ===================== */

    /**
     * Snapshot 단건 조회
     *
     * 사용 목적:
     * - Snapshot 상세 조회
     * - 조회 화면 / 디버깅 / 운영 확인
     */
    fun getSnapshot(snapshotId: Long): JobSnapshotView?

    /**
     * canonicalHash 기준 Snapshot 조회
     *
     * 사용 목적:
     * - 입력 중복 판정
     * - 동일 JD 여부 확인
     */
    fun getSnapshotByContentHash(
        canonicalHash: String
    ): JobSnapshotView?

    /**
     * 브랜드 + 포지션 기준 Snapshot 목록 조회
     *
     * 사용 목적:
     * - 특정 Brand/Position에 대한 히스토리 분석
     */
    fun listSnapshotsForPosition(
        brandId: Long,
        positionId: Long
    ): List<JobSnapshotView>

    /**
     * 브랜드 + 포지션 기준 최신 Snapshot 조회
     *
     * 사용 목적:
     * - 가장 최근 JD 확인
     */
    fun getLatestSnapshotForPosition(
        brandId: Long,
        positionId: Long
    ): JobSnapshotView?

    /* =============================== */
    /* Command 흐름 준비용 Entity 로딩 */
    /* =============================== */

    /**
     * Snapshot Entity 로딩 (Command 전용)
     *
     * 사용 목적:
     * - Snapshot에 분석 결과(brandId / positionId)를 반영하기 전
     * - Domain 메서드를 호출하기 위한 Entity 확보
     *
     * 사용 규칙:
     * - 상태 변경(Command) 유스케이스에서만 사용
     * - 조회/화면/리스트 용도로 사용 ❌
     */
    fun loadSnapshot(snapshotId: Long): JobSnapshot

    /**
     * URL 목록 기준 Snapshot 목록 로딩 (Command 전용)
     */
    fun loadSnapshotsByUrl(url: String): List<JobSnapshot>

    /**
     * 기간(openedDate / closedDate) 기준 Snapshot 목록 로딩 (Command 전용)
     *
     * 규칙:
     * - 입력 기간과 "겹치는" Snapshot만 반환
     * - 판단 로직(중복 여부) 포함 ❌
     * - 단순 필터링만 수행
     */
    fun loadSnapshotsByDateRange(
        openedDate: LocalDate?,
        closedDate: LocalDate?
    ): List<JobSnapshot>
}
