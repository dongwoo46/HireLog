package com.hirelog.api.job.infrastructure.review.adapter

import com.hirelog.api.common.application.port.PagedResult
import com.hirelog.api.job.application.review.port.JobSummaryReviewQuery
import com.hirelog.api.job.application.summary.view.JobSummaryReviewView
import com.hirelog.api.job.domain.model.QJobSummaryReview
import com.hirelog.api.job.domain.type.HiringStage
import com.hirelog.api.member.domain.QMember
import com.querydsl.core.BooleanBuilder
import com.querydsl.core.types.Projections
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Component

/**
 * JobSummaryReview Query Adapter (QueryDSL)
 *
 * 책임:
 * - Query Port를 QueryDSL로 구현
 * - 필터 + 페이징 조회
 * - Member LEFT JOIN으로 회원 정보 포함
 * - anonymous=true → memberId, memberName null
 */
@Component
class JobSummaryReviewJpaQueryAdapter(
    private val queryFactory: JPAQueryFactory
) : JobSummaryReviewQuery {

    private val review = QJobSummaryReview.jobSummaryReview
    private val member = QMember.member

    override fun findByJobSummaryId(
        jobSummaryId: Long,
        hiringStage: HiringStage?,
        minDifficultyRating: Int?,
        maxDifficultyRating: Int?,
        minSatisfactionRating: Int?,
        maxSatisfactionRating: Int?,
        page: Int,
        size: Int
    ): PagedResult<JobSummaryReviewView> {

        require(page >= 0) { "page는 0 이상이어야 합니다." }
        require(size in 1..100) { "size는 1~100 사이여야 합니다." }

        val condition = BooleanBuilder()
            .and(review.jobSummaryId.eq(jobSummaryId))
            .and(review.deleted.isFalse)

        hiringStage?.let { condition.and(review.hiringStage.eq(it)) }
        minDifficultyRating?.let { condition.and(review.difficultyRating.goe(it)) }
        maxDifficultyRating?.let { condition.and(review.difficultyRating.loe(it)) }
        minSatisfactionRating?.let { condition.and(review.satisfactionRating.goe(it)) }
        maxSatisfactionRating?.let { condition.and(review.satisfactionRating.loe(it)) }

        val totalElements = queryFactory
            .select(review.count())
            .from(review)
            .where(condition)
            .fetchOne() ?: 0L

        val items = queryFactory
            .select(
                Projections.constructor(
                    JobSummaryReviewView::class.java,
                    review.id,
                    review.anonymous,
                    review.memberId,
                    member.username,
                    review.hiringStage,
                    review.difficultyRating,
                    review.satisfactionRating,
                    review.experienceComment,
                    review.interviewTip,
                    review.createdAt
                )
            )
            .from(review)
            .leftJoin(member).on(review.memberId.eq(member.id))
            .where(condition)
            .orderBy(review.id.desc())
            .offset((page * size).toLong())
            .limit(size.toLong())
            .fetch()

        return PagedResult.of(
            items = items,
            page = page,
            size = size,
            totalElements = totalElements
        )
    }
}
