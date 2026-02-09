package com.hirelog.api.job.application.summary.port

import com.hirelog.api.job.application.summary.query.JobSummarySearchCondition
import com.hirelog.api.job.application.summary.view.JobSummaryDetailView
import com.hirelog.api.job.application.summary.view.JobSummaryView
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

/**
 * JobSummary 조회 포트
 *
 * 책임:
 * - JobSummary 조회 유스케이스 정의
 * - 저장소 구현(JPA / OpenSearch)을 추상화
 */
interface JobSummaryQuery {

    /**
     * JobSummary 검색
     *
     * @param condition 조회 조건 (유스케이스 모델)
     * @param pageable 페이징 정보
     */
    fun search(
        condition: JobSummarySearchCondition,
        pageable: Pageable
    ): Page<JobSummaryView>

    /**
     * JobSummary 상세 조회 (리뷰 + 사용자 저장 상태 포함)
     *
     * @param jobSummaryId JobSummary ID
     * @param memberId 현재 로그인 사용자 ID
     * @return 상세 View (없으면 null)
     */
    fun findDetailById(jobSummaryId: Long, memberId: Long): JobSummaryDetailView?

    /**
     * URL 기반 중복 체크
     *
     * @param sourceUrl 원본 JD URL
     * @return 해당 URL로 생성된 JobSummary 존재 여부
     */
    fun existsBySourceUrl(sourceUrl: String): Boolean

    /**
     * URL 기반 JobSummary 조회
     *
     * 용도:
     * - 중복 시 기존 JobSummary 반환
     *
     * @param sourceUrl 원본 JD URL
     * @return JobSummary View (없으면 null)
     */
    fun findBySourceUrl(sourceUrl: String): JobSummaryView?

    /**
     * Snapshot 기반 JobSummary 존재 여부 확인
     *
     * 용도:
     * - 중복 체크 시 Snapshot만 있고 Summary 생성 실패한 경우 구분
     * - Snapshot 있음 + Summary 없음 = 재처리 허용
     *
     * @param jobSnapshotId JobSnapshot ID
     * @return 해당 Snapshot으로 생성된 JobSummary 존재 여부
     */
    fun existsByJobSnapshotId(jobSnapshotId: Long): Boolean

    /**
     * Snapshot 기반 JobSummary ID 조회
     *
     * 용도:
     * - 중복 판정 시 기존 JobSummary ID 반환
     *
     * @param jobSnapshotId JobSnapshot ID
     * @return JobSummary ID (없으면 null)
     */
    fun findIdByJobSnapshotId(jobSnapshotId: Long): Long?
}
