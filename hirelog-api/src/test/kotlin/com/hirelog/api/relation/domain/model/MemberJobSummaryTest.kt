package com.hirelog.api.relation.domain.model

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.*

@DisplayName("MemberJobSummary 도메인 테스트 - 자기소개서 관리")
class MemberJobSummaryTest {

    private fun createSummary() = MemberJobSummary.create(
        memberId = 1L,
        jobSummaryId = 100L,
        brandName = "Toss",
        positionName = "Backend",
        brandPositionName = "Backend Engineer",
        positionCategoryName = "Engineering"
    )

    @Nested
    @DisplayName("addCoverLetter는")
    inner class AddCoverLetterTest {

        @Test
        @DisplayName("question과 content를 가진 자기소개서를 추가한다")
        fun shouldAddCoverLetter() {
            val summary = createSummary()
            summary.addCoverLetter("지원 동기", "저는...")

            assertThat(summary.getCoverLetters()).hasSize(1)
            assertThat(summary.getCoverLetters()[0].question).isEqualTo("지원 동기")
            assertThat(summary.getCoverLetters()[0].content).isEqualTo("저는...")
        }

        @Test
        @DisplayName("sortOrder 미지정 시 추가 순서(0-based index)가 sortOrder가 된다")
        fun shouldAssignDefaultSortOrder() {
            val summary = createSummary()
            summary.addCoverLetter("1번", "내용1")
            summary.addCoverLetter("2번", "내용2")

            // getCoverLetters()는 sortOrder 오름차순 반환
            val letters = summary.getCoverLetters()
            assertThat(letters[0].sortOrder).isEqualTo(0)
            assertThat(letters[1].sortOrder).isEqualTo(1)
        }

        @Test
        @DisplayName("sortOrder를 명시적으로 지정할 수 있다")
        fun shouldUseExplicitSortOrder() {
            val summary = createSummary()
            summary.addCoverLetter("질문", "내용", 5)

            assertThat(summary.getCoverLetters()[0].sortOrder).isEqualTo(5)
        }

        @Test
        @DisplayName("여러 개를 추가할 수 있다")
        fun shouldAddMultipleCoverLetters() {
            val summary = createSummary()
            summary.addCoverLetter("질문1", "내용1")
            summary.addCoverLetter("질문2", "내용2")
            summary.addCoverLetter("질문3", "내용3")

            assertThat(summary.getCoverLetters()).hasSize(3)
        }

        @Test
        @DisplayName("question이 빈 값이면 IllegalArgumentException을 던진다")
        fun shouldThrowWhenQuestionBlank() {
            val summary = createSummary()

            assertThatThrownBy {
                summary.addCoverLetter("", "내용")
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("question")
        }

        @Test
        @DisplayName("content가 빈 값이면 IllegalArgumentException을 던진다")
        fun shouldThrowWhenContentBlank() {
            val summary = createSummary()

            assertThatThrownBy {
                summary.addCoverLetter("질문", "")
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("content")
        }
    }

    @Nested
    @DisplayName("updateCoverLetter는")
    inner class UpdateCoverLetterTest {

        @Test
        @DisplayName("존재하는 자기소개서의 question, content, sortOrder를 수정한다")
        fun shouldUpdateCoverLetter() {
            val summary = createSummary()
            summary.addCoverLetter("원래 질문", "원래 내용")

            val letterId = summary.getCoverLetters()[0].id  // id=0 (DB 미사용)
            summary.updateCoverLetter(letterId, "수정된 질문", "수정된 내용", 3)

            val updated = summary.getCoverLetters()[0]
            assertThat(updated.question).isEqualTo("수정된 질문")
            assertThat(updated.content).isEqualTo("수정된 내용")
            assertThat(updated.sortOrder).isEqualTo(3)
        }

        @Test
        @DisplayName("존재하지 않는 coverLetterId이면 IllegalStateException을 던진다")
        fun shouldThrowWhenCoverLetterNotFound() {
            val summary = createSummary()

            assertThatThrownBy {
                summary.updateCoverLetter(9999L, "질문", "내용", 0)
            }.isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("CoverLetter not found")
        }

        @Test
        @DisplayName("question이 빈 값이면 IllegalArgumentException을 던진다")
        fun shouldThrowWhenQuestionBlank() {
            val summary = createSummary()
            summary.addCoverLetter("질문", "내용")
            val letterId = summary.getCoverLetters()[0].id

            assertThatThrownBy {
                summary.updateCoverLetter(letterId, "", "내용", 0)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("question")
        }

        @Test
        @DisplayName("content가 빈 값이면 IllegalArgumentException을 던진다")
        fun shouldThrowWhenContentBlank() {
            val summary = createSummary()
            summary.addCoverLetter("질문", "내용")
            val letterId = summary.getCoverLetters()[0].id

            assertThatThrownBy {
                summary.updateCoverLetter(letterId, "질문", "", 0)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("content")
        }
    }

    @Nested
    @DisplayName("removeCoverLetter는")
    inner class RemoveCoverLetterTest {

        @Test
        @DisplayName("지정한 id의 자기소개서를 삭제한다")
        fun shouldRemoveCoverLetter() {
            val summary = createSummary()
            summary.addCoverLetter("질문", "내용")

            val letterId = summary.getCoverLetters()[0].id
            summary.removeCoverLetter(letterId)

            assertThat(summary.getCoverLetters()).isEmpty()
        }

        @Test
        @DisplayName("존재하지 않는 id로 삭제 시 무시한다 (멱등)")
        fun shouldBeIdempotentWhenNotFound() {
            val summary = createSummary()
            summary.addCoverLetter("질문", "내용")

            summary.removeCoverLetter(9999L)

            assertThat(summary.getCoverLetters()).hasSize(1)
        }

        // 특정 항목만 삭제하고 나머지를 유지하는 동작은 id 고유성에 의존한다.
        // CoverLetter.id는 DB BIGSERIAL이므로 단위 테스트 환경(DB 없음)에서는
        // 모든 인스턴스의 id=0으로 동일하여 검증 불가 → 통합 테스트 영역
    }

    @Nested
    @DisplayName("getCoverLetters는")
    inner class GetCoverLettersTest {

        @Test
        @DisplayName("sortOrder 오름차순으로 정렬하여 반환한다")
        fun shouldReturnSortedBySortOrder() {
            val summary = createSummary()
            summary.addCoverLetter("3번", "내용3", 3)
            summary.addCoverLetter("1번", "내용1", 1)
            summary.addCoverLetter("2번", "내용2", 2)

            val letters = summary.getCoverLetters()
            assertThat(letters[0].question).isEqualTo("1번")
            assertThat(letters[1].question).isEqualTo("2번")
            assertThat(letters[2].question).isEqualTo("3번")
        }

        @Test
        @DisplayName("추가된 항목이 없으면 빈 리스트를 반환한다")
        fun shouldReturnEmptyWhenNone() {
            val summary = createSummary()

            assertThat(summary.getCoverLetters()).isEmpty()
        }
    }
}
