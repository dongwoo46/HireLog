package com.hirelog.api.job.infrastructure.review.adapter

import com.hirelog.api.common.application.port.PagedResult
import com.hirelog.api.job.application.review.port.JobSummaryReviewQuery
import com.hirelog.api.job.application.summary.view.JobSummaryReviewView
import com.hirelog.api.job.domain.model.QJobSummary
import com.hirelog.api.job.domain.model.QJobSummaryReview
import com.hirelog.api.job.domain.type.HiringStage
import com.hirelog.api.job.domain.type.ReviewSortType
import com.hirelog.api.member.domain.QMember
import com.hirelog.api.relation.domain.model.QReviewLike
import com.querydsl.core.BooleanBuilder
import com.querydsl.core.types.OrderSpecifier
import com.querydsl.core.types.Projections
import com.querydsl.core.types.dsl.Expressions
import com.querydsl.core.types.dsl.NumberExpression
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class JobSummaryReviewJpaQueryAdapter(
    private val queryFactory: JPAQueryFactory
) : JobSummaryReviewQuery {

    private val review = QJobSummaryReview.jobSummaryReview
    private val summary = QJobSummary.jobSummary
    private val member = QMember.member
    private val reviewLike = QReviewLike.reviewLike

    override fun findByJobSummaryId(
        jobSummaryId: Long,
        hiringStage: HiringStage?,
        minDifficultyRating: Int?,
        maxDifficultyRating: Int?,
        minSatisfactionRating: Int?,
        maxSatisfactionRating: Int?,
        sortBy: ReviewSortType,
        createdFrom: LocalDate?,
        createdTo: LocalDate?,
        includeDeleted: Boolean,
        page: Int,
        size: Int
    ): PagedResult<JobSummaryReviewView> {
        val condition = buildCondition(
            jobSummaryId = jobSummaryId,
            memberName = null,
            hiringStage = hiringStage,
            minDifficultyRating = minDifficultyRating,
            maxDifficultyRating = maxDifficultyRating,
            minSatisfactionRating = minSatisfactionRating,
            maxSatisfactionRating = maxSatisfactionRating,
            createdFrom = createdFrom,
            createdTo = createdTo,
            includeDeleted = includeDeleted
        )
        return search(condition, sortBy, page, size)
    }

    override fun findAll(
        jobSummaryId: Long?,
        memberName: String?,
        hiringStage: HiringStage?,
        minDifficultyRating: Int?,
        maxDifficultyRating: Int?,
        minSatisfactionRating: Int?,
        maxSatisfactionRating: Int?,
        sortBy: ReviewSortType,
        createdFrom: LocalDate?,
        createdTo: LocalDate?,
        includeDeleted: Boolean,
        page: Int,
        size: Int
    ): PagedResult<JobSummaryReviewView> {
        val condition = buildCondition(
            jobSummaryId = jobSummaryId,
            memberName = memberName,
            hiringStage = hiringStage,
            minDifficultyRating = minDifficultyRating,
            maxDifficultyRating = maxDifficultyRating,
            minSatisfactionRating = minSatisfactionRating,
            maxSatisfactionRating = maxSatisfactionRating,
            createdFrom = createdFrom,
            createdTo = createdTo,
            includeDeleted = includeDeleted
        )
        return search(condition, sortBy, page, size)
    }

    private fun buildCondition(
        jobSummaryId: Long?,
        memberName: String?,
        hiringStage: HiringStage?,
        minDifficultyRating: Int?,
        maxDifficultyRating: Int?,
        minSatisfactionRating: Int?,
        maxSatisfactionRating: Int?,
        createdFrom: LocalDate?,
        createdTo: LocalDate?,
        includeDeleted: Boolean
    ): BooleanBuilder {
        val condition = BooleanBuilder()

        if (!includeDeleted) {
            condition.and(review.deleted.isFalse)
        }

        jobSummaryId?.let { condition.and(review.jobSummaryId.eq(it)) }
        memberName?.takeIf { it.isNotBlank() }?.let { condition.and(member.username.containsIgnoreCase(it)) }
        hiringStage?.let { condition.and(review.hiringStage.eq(it)) }
        minDifficultyRating?.let { condition.and(review.difficultyRating.goe(it)) }
        maxDifficultyRating?.let { condition.and(review.difficultyRating.loe(it)) }
        minSatisfactionRating?.let { condition.and(review.satisfactionRating.goe(it)) }
        maxSatisfactionRating?.let { condition.and(review.satisfactionRating.loe(it)) }
        createdFrom?.let { condition.and(review.createdAt.goe(it.atStartOfDay())) }
        createdTo?.let { condition.and(review.createdAt.lt(it.plusDays(1).atStartOfDay())) }

        return condition
    }

    private fun search(
        condition: BooleanBuilder,
        sortBy: ReviewSortType,
        page: Int,
        size: Int
    ): PagedResult<JobSummaryReviewView> {
        require(page >= 0) { "page must be >= 0" }
        require(size in 1..100) { "size must be between 1 and 100" }

        val likeCount: NumberExpression<Long> = reviewLike.id.countDistinct()

        val totalElements = queryFactory
            .select(review.id.countDistinct())
            .from(review)
            .leftJoin(member).on(review.memberId.eq(member.id))
            .where(condition)
            .fetchOne() ?: 0L

        val orderBy: Array<OrderSpecifier<*>> = when (sortBy) {
            ReviewSortType.LIKES -> arrayOf(likeCount.desc(), review.id.desc())
            ReviewSortType.RATING -> arrayOf(
                Expressions.numberTemplate(
                    Int::class.java,
                    "({0} + {1})",
                    review.difficultyRating,
                    review.satisfactionRating
                ).desc(),
                review.id.desc()
            )
            ReviewSortType.DIFFICULTY -> arrayOf(review.difficultyRating.desc(), review.id.desc())
            ReviewSortType.SATISFACTION -> arrayOf(review.satisfactionRating.desc(), review.id.desc())
            ReviewSortType.LATEST -> arrayOf(review.id.desc())
        }

        val items = queryFactory
            .select(
                Projections.constructor(
                    JobSummaryReviewView::class.java,
                    review.id,
                    review.jobSummaryId,
                    summary.companyName,
                    summary.brandPositionName,
                    review.anonymous,
                    review.memberId,
                    member.username,
                    review.hiringStage,
                    review.difficultyRating,
                    review.satisfactionRating,
                    review.prosComment,
                    review.consComment,
                    review.tip,
                    likeCount,
                    review.deleted,
                    review.createdAt
                )
            )
            .from(review)
            .leftJoin(summary).on(review.jobSummaryId.eq(summary.id))
            .leftJoin(member).on(review.memberId.eq(member.id))
            .leftJoin(reviewLike).on(reviewLike.reviewId.eq(review.id))
            .where(condition)
            .groupBy(
                review.id,
                review.jobSummaryId,
                summary.companyName,
                summary.brandPositionName,
                review.anonymous,
                review.memberId,
                member.username,
                review.hiringStage,
                review.difficultyRating,
                review.satisfactionRating,
                review.prosComment,
                review.consComment,
                review.tip,
                review.deleted,
                review.createdAt
            )
            .orderBy(*orderBy)
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
