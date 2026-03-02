package com.hirelog.api.relation.application.memberjobsummary

import com.hirelog.api.relation.domain.model.MemberJobSummary
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.*

@DisplayName("MemberJobSummaryWriteService - CoverLetter CRUD 테스트")
class MemberJobSummaryWriteServiceTest {

    private lateinit var service: MemberJobSummaryWriteService
    private lateinit var command: MemberJobSummaryCommand

    private fun createSummary() = MemberJobSummary.create(
        memberId = 1L,
        jobSummaryId = 100L,
        brandName = "Toss",
        positionName = "Backend",
        brandPositionName = "Backend Engineer",
        positionCategoryName = "Engineering"
    )

    @BeforeEach
    fun setUp() {
        command = mockk(relaxed = true)
        service = MemberJobSummaryWriteService(command)
    }

    @Nested
    @DisplayName("addCoverLetter는")
    inner class AddCoverLetterTest {

        @Test
        @DisplayName("sortOrder 없이 자기소개서를 추가하고 aggregate를 저장한다")
        fun shouldAddCoverLetterWithDefaultSortOrderAndSave() {
            val summary = createSummary()
            every { command.findEntityByMemberIdAndJobSummaryId(1L, 100L) } returns summary

            service.addCoverLetter(1L, 100L, "지원 동기", "저는...", null)

            verify { command.save(match { it.getCoverLetters().size == 1 }) }
            assertThat(summary.getCoverLetters()[0].question).isEqualTo("지원 동기")
        }

        @Test
        @DisplayName("sortOrder를 명시적으로 지정하면 해당 sortOrder로 추가한다")
        fun shouldAddCoverLetterWithExplicitSortOrder() {
            val summary = createSummary()
            every { command.findEntityByMemberIdAndJobSummaryId(1L, 100L) } returns summary

            service.addCoverLetter(1L, 100L, "지원 동기", "저는...", 5)

            verify { command.save(match { it.getCoverLetters()[0].sortOrder == 5 }) }
        }

        @Test
        @DisplayName("MemberJobSummary가 없으면 IllegalStateException을 던진다")
        fun shouldThrowWhenNotFound() {
            every { command.findEntityByMemberIdAndJobSummaryId(1L, 100L) } returns null

            assertThatThrownBy {
                service.addCoverLetter(1L, 100L, "질문", "내용", null)
            }.isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("MemberJobSummary not found")
        }
    }

    @Nested
    @DisplayName("updateCoverLetter는")
    inner class UpdateCoverLetterTest {

        @Test
        @DisplayName("존재하는 자기소개서의 내용을 수정하고 aggregate를 저장한다")
        fun shouldUpdateCoverLetterAndSave() {
            val summary = createSummary()
            summary.addCoverLetter("원래 질문", "원래 내용")
            val letterId = summary.getCoverLetters()[0].id  // id=0 (DB 미사용)

            every { command.findEntityByMemberIdAndJobSummaryId(1L, 100L) } returns summary

            service.updateCoverLetter(1L, 100L, letterId, "수정된 질문", "수정된 내용", 2)

            verify { command.save(summary) }
            assertThat(summary.getCoverLetters()[0].question).isEqualTo("수정된 질문")
            assertThat(summary.getCoverLetters()[0].sortOrder).isEqualTo(2)
        }

        @Test
        @DisplayName("MemberJobSummary가 없으면 IllegalStateException을 던진다")
        fun shouldThrowWhenNotFound() {
            every { command.findEntityByMemberIdAndJobSummaryId(1L, 100L) } returns null

            assertThatThrownBy {
                service.updateCoverLetter(1L, 100L, 0L, "질문", "내용", 0)
            }.isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("MemberJobSummary not found")
        }

        @Test
        @DisplayName("존재하지 않는 coverLetterId이면 IllegalStateException을 던진다")
        fun shouldThrowWhenCoverLetterNotFound() {
            val summary = createSummary()
            every { command.findEntityByMemberIdAndJobSummaryId(1L, 100L) } returns summary

            assertThatThrownBy {
                service.updateCoverLetter(1L, 100L, 9999L, "질문", "내용", 0)
            }.isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("CoverLetter not found")
        }
    }

    @Nested
    @DisplayName("removeCoverLetter는")
    inner class RemoveCoverLetterTest {

        @Test
        @DisplayName("자기소개서를 삭제하고 aggregate를 저장한다")
        fun shouldRemoveCoverLetterAndSave() {
            val summary = createSummary()
            summary.addCoverLetter("질문", "내용")
            val letterId = summary.getCoverLetters()[0].id

            every { command.findEntityByMemberIdAndJobSummaryId(1L, 100L) } returns summary

            service.removeCoverLetter(1L, 100L, letterId)

            verify { command.save(match { it.getCoverLetters().isEmpty() }) }
        }

        @Test
        @DisplayName("MemberJobSummary가 없으면 IllegalStateException을 던진다")
        fun shouldThrowWhenNotFound() {
            every { command.findEntityByMemberIdAndJobSummaryId(1L, 100L) } returns null

            assertThatThrownBy {
                service.removeCoverLetter(1L, 100L, 0L)
            }.isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("MemberJobSummary not found")
        }
    }
}
