package com.hirelog.api.member.domain

import com.hirelog.api.member.domain.vo.ReviewContent
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.*

@DisplayName("ReviewContent 도메인 테스트")
class ReviewContentTest {

    @Nested
    @DisplayName("of 팩토리는")
    inner class OfTest {

        @Test
        @DisplayName("feeling만 있어도 생성된다")
        fun shouldCreateWithFeelingOnly() {
            val content = ReviewContent.of(feeling = "재밌어 보임")
            assertThat(content.feeling).isEqualTo("재밌어 보임")
            assertThat(content.tip).isNull()
            assertThat(content.experience).isNull()
        }

        @Test
        @DisplayName("tip만 있어도 생성된다")
        fun shouldCreateWithTipOnly() {
            val content = ReviewContent.of(tip = "CS 위주로 준비")
            assertThat(content.tip).isEqualTo("CS 위주로 준비")
        }

        @Test
        @DisplayName("experience만 있어도 생성된다")
        fun shouldCreateWithExperienceOnly() {
            val content = ReviewContent.of(experience = "비슷한 포지션 경험 있음")
            assertThat(content.experience).isEqualTo("비슷한 포지션 경험 있음")
        }

        @Test
        @DisplayName("세 필드 모두 있으면 생성된다")
        fun shouldCreateWithAllFields() {
            val content = ReviewContent.of(
                feeling = "좋아 보임",
                tip = "알고리즘 준비",
                experience = "토스 경험 있음"
            )
            assertThat(content.feeling).isEqualTo("좋아 보임")
            assertThat(content.tip).isEqualTo("알고리즘 준비")
            assertThat(content.experience).isEqualTo("토스 경험 있음")
        }

        @Test
        @DisplayName("세 필드 모두 null이면 예외를 던진다")
        fun shouldThrowWhenAllNull() {
            assertThatThrownBy {
                ReviewContent.of(feeling = null, tip = null, experience = null)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("at least one field")
        }
    }
}
