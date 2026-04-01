package com.hirelog.api.job.application.review

import com.hirelog.api.job.application.review.port.ReviewLikeQuery
import com.hirelog.api.job.application.review.view.ReviewLikeStatView
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("ReviewLikeReadService 테스트")
class ReviewLikeReadServiceTest {

    private lateinit var service: ReviewLikeReadService
    private lateinit var query: ReviewLikeQuery

    @BeforeEach
    fun setUp() {
        query = mockk()
        service = ReviewLikeReadService(query)
    }

    @Test
    @DisplayName("조회 시 포트 결과를 그대로 반환한다")
    fun returnsStat() {
        every { query.findStat(10L, 1L) } returns ReviewLikeStatView(
            reviewId = 10L,
            likeCount = 7L,
            likedByMe = true
        )

        val result = service.getStat(reviewId = 10L, memberId = 1L)

        assertThat(result.reviewId).isEqualTo(10L)
        assertThat(result.likeCount).isEqualTo(7L)
        assertThat(result.likedByMe).isTrue()
        verify(exactly = 1) { query.findStat(10L, 1L) }
    }
}
