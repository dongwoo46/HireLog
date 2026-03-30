package com.hirelog.api.relation.application.memberjobsummary

import com.hirelog.api.relation.domain.model.MemberJobSummary
import com.hirelog.api.relation.domain.type.MemberJobSummarySaveType
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.*

@DisplayName("MemberJobSummaryWriteService - CoverLetter CRUD ?뚯뒪??)
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
    @DisplayName("save")
    inner class SaveTest {

        @Test
        @DisplayName("restores archived summary when save is requested again")
        fun shouldRestoreArchivedSummary() {
            val summary = createSummary()
            summary.changeStatus(MemberJobSummarySaveType.UNSAVED)
            every { command.findEntityByMemberIdAndJobSummaryId(1L, 100L) } returns summary

            service.save(
                com.hirelog.api.relation.application.view.CreateMemberJobSummaryCommand(
                    memberId = 1L,
                    jobSummaryId = 100L,
                    brandName = "Toss",
                    positionName = "Backend",
                    brandPositionName = "Backend Engineer",
                    positionCategoryName = "Engineering"
                )
            )

            assertThat(summary.saveType).isEqualTo(MemberJobSummarySaveType.SAVED)
            verify { command.save(summary) }
        }
    }

    @Nested
    @DisplayName("addCoverLetter??)
    inner class AddCoverLetterTest {

        @Test
        @DisplayName("sortOrder ?놁씠 ?먭린?뚭컻?쒕? 異붽??섍퀬 aggregate瑜???ν븳??)
        fun shouldAddCoverLetterWithDefaultSortOrderAndSave() {
            val summary = createSummary()
            every { command.findEntityByMemberIdAndJobSummaryId(1L, 100L) } returns summary

            service.addCoverLetter(1L, 100L, "吏???숆린", "???..", null)

            verify { command.save(match { it.getCoverLetters().size == 1 }) }
            assertThat(summary.getCoverLetters()[0].question).isEqualTo("吏???숆린")
        }

        @Test
        @DisplayName("sortOrder瑜?紐낆떆?곸쑝濡?吏?뺥븯硫??대떦 sortOrder濡?異붽??쒕떎")
        fun shouldAddCoverLetterWithExplicitSortOrder() {
            val summary = createSummary()
            every { command.findEntityByMemberIdAndJobSummaryId(1L, 100L) } returns summary

            service.addCoverLetter(1L, 100L, "吏???숆린", "???..", 5)

            verify { command.save(match { it.getCoverLetters()[0].sortOrder == 5 }) }
        }

        @Test
        @DisplayName("MemberJobSummary媛 ?놁쑝硫?IllegalStateException???섏쭊??)
        fun shouldThrowWhenNotFound() {
            every { command.findEntityByMemberIdAndJobSummaryId(1L, 100L) } returns null

            assertThatThrownBy {
                service.addCoverLetter(1L, 100L, "吏덈Ц", "?댁슜", null)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("MemberJobSummary not found")
        }
    }

    @Nested
    @DisplayName("updateCoverLetter??)
    inner class UpdateCoverLetterTest {

        @Test
        @DisplayName("議댁옱?섎뒗 ?먭린?뚭컻?쒖쓽 ?댁슜???섏젙?섍퀬 aggregate瑜???ν븳??)
        fun shouldUpdateCoverLetterAndSave() {
            val summary = createSummary()
            summary.addCoverLetter("?먮옒 吏덈Ц", "?먮옒 ?댁슜")
            val letterId = summary.getCoverLetters()[0].id  // id=0 (DB 誘몄궗??

            every { command.findEntityByMemberIdAndJobSummaryId(1L, 100L) } returns summary

            service.updateCoverLetter(1L, 100L, letterId, "?섏젙??吏덈Ц", "?섏젙???댁슜", 2)

            verify { command.save(summary) }
            assertThat(summary.getCoverLetters()[0].question).isEqualTo("?섏젙??吏덈Ц")
            assertThat(summary.getCoverLetters()[0].sortOrder).isEqualTo(2)
        }

        @Test
        @DisplayName("MemberJobSummary媛 ?놁쑝硫?IllegalStateException???섏쭊??)
        fun shouldThrowWhenNotFound() {
            every { command.findEntityByMemberIdAndJobSummaryId(1L, 100L) } returns null

            assertThatThrownBy {
                service.updateCoverLetter(1L, 100L, 0L, "吏덈Ц", "?댁슜", 0)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("MemberJobSummary not found")
        }

        @Test
        @DisplayName("議댁옱?섏? ?딅뒗 coverLetterId?대㈃ IllegalStateException???섏쭊??)
        fun shouldThrowWhenCoverLetterNotFound() {
            val summary = createSummary()
            every { command.findEntityByMemberIdAndJobSummaryId(1L, 100L) } returns summary

            assertThatThrownBy {
                service.updateCoverLetter(1L, 100L, 9999L, "吏덈Ц", "?댁슜", 0)
            }.isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("CoverLetter not found")
        }
    }

    @Nested
    @DisplayName("removeCoverLetter??)
    inner class RemoveCoverLetterTest {

        @Test
        @DisplayName("?먭린?뚭컻?쒕? ??젣?섍퀬 aggregate瑜???ν븳??)
        fun shouldRemoveCoverLetterAndSave() {
            val summary = createSummary()
            summary.addCoverLetter("吏덈Ц", "?댁슜")
            val letterId = summary.getCoverLetters()[0].id

            every { command.findEntityByMemberIdAndJobSummaryId(1L, 100L) } returns summary

            service.removeCoverLetter(1L, 100L, letterId)

            verify { command.save(match { it.getCoverLetters().isEmpty() }) }
        }

        @Test
        @DisplayName("MemberJobSummary媛 ?놁쑝硫?IllegalStateException???섏쭊??)
        fun shouldThrowWhenNotFound() {
            every { command.findEntityByMemberIdAndJobSummaryId(1L, 100L) } returns null

            assertThatThrownBy {
                service.removeCoverLetter(1L, 100L, 0L)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("MemberJobSummary not found")
        }
    }
}

