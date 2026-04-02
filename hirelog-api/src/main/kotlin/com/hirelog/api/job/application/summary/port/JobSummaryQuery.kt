package com.hirelog.api.job.application.summary.port

import com.hirelog.api.job.application.summary.query.JobSummarySearchCondition
import com.hirelog.api.job.application.summary.view.JobSummaryAdminView
import com.hirelog.api.job.application.summary.view.JobSummaryDetailView
import com.hirelog.api.job.application.summary.view.JobSummaryView
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

/**
 * JobSummary 議고쉶 ?ы듃
 *
 * 梨낆엫:
 * - JobSummary 議고쉶 ?좎뒪耳?댁뒪 ?뺤쓽
 * - ??μ냼 援ы쁽(JPA / OpenSearch)??異붿긽?? */
interface JobSummaryQuery {

    /**
     * JobSummary 寃??     *
     * @param condition 議고쉶 議곌굔 (?좎뒪耳?댁뒪 紐⑤뜽)
     * @param pageable ?섏씠吏??뺣낫
     */
    fun search(
        condition: JobSummarySearchCondition,
        pageable: Pageable
    ): Page<JobSummaryView>

    /**
     * JobSummary ?곸꽭 議고쉶 (由щ럭 + ?ъ슜??????곹깭 ?ы븿)
     *
     * @param jobSummaryId JobSummary ID
     * @param memberId ?꾩옱 濡쒓렇???ъ슜??ID
     * @return ?곸꽭 View (?놁쑝硫?null)
     */
    fun findDetailById(jobSummaryId: Long, memberId: Long?): JobSummaryDetailView?

    /**
     * URL 湲곕컲 以묐났 泥댄겕
     *
     * @param sourceUrl ?먮낯 JD URL
     * @return ?대떦 URL濡??앹꽦??JobSummary 議댁옱 ?щ?
     */
    fun existsBySourceUrl(sourceUrl: String): Boolean

    /**
     * URL 湲곕컲 JobSummary 議고쉶
     *
     * ?⑸룄:
     * - 以묐났 ??湲곗〈 JobSummary 諛섑솚
     *
     * @param sourceUrl ?먮낯 JD URL
     * @return JobSummary View (?놁쑝硫?null)
     */
    fun findBySourceUrl(sourceUrl: String): JobSummaryView?

    /**
     * Snapshot 湲곕컲 JobSummary 議댁옱 ?щ? ?뺤씤
     *
     * ?⑸룄:
     * - 以묐났 泥댄겕 ??Snapshot留??덇퀬 Summary ?앹꽦 ?ㅽ뙣??寃쎌슦 援щ텇
     * - Snapshot ?덉쓬 + Summary ?놁쓬 = ?ъ쿂由??덉슜
     *
     * @param jobSnapshotId JobSnapshot ID
     * @return ?대떦 Snapshot?쇰줈 ?앹꽦??JobSummary 議댁옱 ?щ?
     */
    fun existsByJobSnapshotId(jobSnapshotId: Long): Boolean

    /**
     * Snapshot 湲곕컲 JobSummary ID 議고쉶
     *
     * ?⑸룄:
     * - 以묐났 ?먯젙 ??湲곗〈 JobSummary ID 諛섑솚
     *
     * @param jobSnapshotId JobSnapshot ID
     * @return JobSummary ID (?놁쑝硫?null)
     */
    fun findIdByJobSnapshotId(jobSnapshotId: Long): Long?

    /**
     * Admin ?꾩슜 ?곸꽭 議고쉶 - isActive 臾닿?
     */
    fun findDetailByIdAdmin(jobSummaryId: Long): JobSummaryDetailView?

    /**
     * Admin ?꾩슜 紐⑸줉 議고쉶 - isActive ?꾪꽣 ?좏깮 媛??(null = ?꾩껜)
     */
    fun searchAdmin(isActive: Boolean?, brandName: String?, pageable: Pageable): Page<JobSummaryAdminView>
}

