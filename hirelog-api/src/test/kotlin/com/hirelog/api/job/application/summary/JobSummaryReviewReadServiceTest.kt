package com.hirelog.api.job.application.summary

import com.hirelog.api.common.application.port.PagedResult
import com.hirelog.api.job.application.review.port.JobSummaryReviewQuery
import com.hirelog.api.job.application.summary.view.JobSummaryReviewView
import com.hirelog.api.job.domain.type.HiringStage
import com.hirelog.api.job.domain.type.ReviewSortType
import com.hirelog.api.relation.application.memberjobsummary.port.MemberJobSummaryQuery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

@DisplayName("JobSummaryReviewReadService 테스트")
class JobSummaryReviewReadServiceTest {

    private lateinit var service: JobSummaryReviewReadService
    private lateinit var query: JobSummaryReviewQuery
    private lateinit var memberJobSummaryQuery: MemberJobSummaryQuery

    @BeforeEach
    fun setUp() {
        query = mockk()
        memberJobSummaryQuery = mockk()
        every { memberJobSummaryQuery.existsAnyByMemberId(any()) } returns true
        service = JobSummaryReviewReadService(query, memberJobSummaryQuery)
    }

    private fun makeView(
        reviewId: Long,
        anonymous: Boolean,
        memberId: Long? = 100L,
        memberName: String? = "홍길동"
    ) = JobSummaryReviewView(
        reviewId = reviewId,
        jobSummaryId = 1L,
        brandName = "카카오",
        brandPositionName = "Backend Engineer",
        anonymous = anonymous,
        memberId = memberId,
        memberName = memberName,
        hiringStage = HiringStage.FINAL_INTERVIEW,
        difficultyRating = 7,
        satisfactionRating = 8,
        prosComment = "좋았어요",
        consComment = "아쉬웠어요",
        tip = "준비 잘하세요",
        likeCount = 3L,
        deleted = false,
        createdAt = LocalDateTime.now()
    )

    @Nested
    @DisplayName("findByJobSummaryId는")
    inner class FindByJobSummaryIdTest {

        @Test
        @DisplayName("anonymous=false 리뷰는 memberId와 memberName을 그대로 노출한다")
        fun shouldExposeIdentityWhenNotAnonymous() {
            val view = makeView(reviewId = 1L, anonymous = false, memberId = 100L, memberName = "홍길동")
            every {
                query.findByJobSummaryId(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
            } returns PagedResult.of(listOf(view), page = 0, size = 10, totalElements = 1L)

            val result = service.findByJobSummaryId(
                memberId = 10L,
                jobSummaryId = 1L,
                hiringStage = null,
                minDifficultyRating = null,
                maxDifficultyRating = null,
                minSatisfactionRating = null,
                maxSatisfactionRating = null,
                sortBy = ReviewSortType.LATEST,
                createdFrom = null,
                createdTo = null,
                includeDeleted = false,
                page = 0,
                size = 10
            )

            val item = result.items.first()
            assertThat(item.anonymous).isFalse()
            assertThat(item.memberId).isEqualTo(100L)
            assertThat(item.memberName).isEqualTo("홍길동")
            assertThat(item.likeCount).isEqualTo(3L)
        }

        @Test
        @DisplayName("anonymous=true 리뷰는 memberId와 memberName을 null로 마스킹한다")
        fun shouldMaskIdentityWhenAnonymous() {
            val view = makeView(reviewId = 2L, anonymous = true, memberId = 100L, memberName = "홍길동")
            every {
                query.findByJobSummaryId(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
            } returns PagedResult.of(listOf(view), page = 0, size = 10, totalElements = 1L)

            val result = service.findByJobSummaryId(
                memberId = 10L,
                jobSummaryId = 1L,
                hiringStage = null,
                minDifficultyRating = null,
                maxDifficultyRating = null,
                minSatisfactionRating = null,
                maxSatisfactionRating = null,
                sortBy = ReviewSortType.LATEST,
                createdFrom = null,
                createdTo = null,
                includeDeleted = false,
                page = 0,
                size = 10
            )

            val item = result.items.first()
            assertThat(item.anonymous).isTrue()
            assertThat(item.memberId).isNull()
            assertThat(item.memberName).isNull()
        }

        @Test
        @DisplayName("필터 조건을 쿼리에 그대로 전달한다")
        fun shouldPassFilterConditionsToQuery() {
            every {
                query.findByJobSummaryId(
                    jobSummaryId = 5L,
                    hiringStage = HiringStage.INTERVIEW_1,
                    minDifficultyRating = 3,
                    maxDifficultyRating = 8,
                    minSatisfactionRating = null,
                    maxSatisfactionRating = null,
                    sortBy = ReviewSortType.LIKES,
                    createdFrom = null,
                    createdTo = null,
                    includeDeleted = false,
                    page = 1,
                    size = 20
                )
            } returns PagedResult.of(emptyList(), page = 1, size = 20, totalElements = 0L)

            service.findByJobSummaryId(
                memberId = 10L,
                jobSummaryId = 5L,
                hiringStage = HiringStage.INTERVIEW_1,
                minDifficultyRating = 3,
                maxDifficultyRating = 8,
                minSatisfactionRating = null,
                maxSatisfactionRating = null,
                sortBy = ReviewSortType.LIKES,
                createdFrom = null,
                createdTo = null,
                includeDeleted = false,
                page = 1,
                size = 20
            )

            verify {
                query.findByJobSummaryId(
                    jobSummaryId = 5L,
                    hiringStage = HiringStage.INTERVIEW_1,
                    minDifficultyRating = 3,
                    maxDifficultyRating = 8,
                    minSatisfactionRating = null,
                    maxSatisfactionRating = null,
                    sortBy = ReviewSortType.LIKES,
                    createdFrom = null,
                    createdTo = null,
                    includeDeleted = false,
                    page = 1,
                    size = 20
                )
            }
        }
    }
}
