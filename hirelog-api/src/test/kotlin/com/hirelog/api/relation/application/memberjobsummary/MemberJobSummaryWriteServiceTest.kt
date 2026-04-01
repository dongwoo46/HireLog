package com.hirelog.api.relation.application.memberjobsummary

import com.hirelog.api.job.application.summary.port.JobSummaryCommand
import com.hirelog.api.job.domain.model.JobSummary
import com.hirelog.api.relation.domain.model.MemberJobSummary
import com.hirelog.api.relation.domain.type.MemberJobSummarySaveType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("MemberJobSummaryWriteService 테스트")
class MemberJobSummaryWriteServiceTest {

    private lateinit var service: MemberJobSummaryWriteService
    private lateinit var command: MemberJobSummaryCommand
    private lateinit var jobSummaryCommand: JobSummaryCommand

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
        jobSummaryCommand = mockk(relaxed = true)
        service = MemberJobSummaryWriteService(command, jobSummaryCommand)
    }

    @Nested
    @DisplayName("save")
    inner class SaveTest {

        @Test
        @DisplayName("UNSAVED 상태의 기존 요약은 save 호출 시 SAVED로 복구된다")
        fun restoresArchivedSummary() {
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
    @DisplayName("changeSaveType")
    inner class ChangeSaveTypeTest {

        @Test
        @DisplayName("요약이 없고 SAVED 요청이면 MemberJobSummary를 생성해 저장한다")
        fun createsWhenMissingAndSaved() {
            val summary = mockk<JobSummary>()
            every { summary.id } returns 100L
            every { summary.brandName } returns "Toss"
            every { summary.positionName } returns "Backend"
            every { summary.brandPositionName } returns "Backend Engineer"
            every { summary.positionCategoryName } returns "Engineering"
            every { command.findEntityByMemberIdAndJobSummaryId(1L, 100L) } returns null
            every { jobSummaryCommand.findById(100L) } returns summary

            service.changeSaveType(1L, 100L, MemberJobSummarySaveType.SAVED)

            verify {
                command.save(match {
                    it.memberId == 1L &&
                        it.jobSummaryId == 100L &&
                        it.saveType == MemberJobSummarySaveType.SAVED
                })
            }
        }

        @Test
        @DisplayName("요약이 없고 APPLY 요청이면 예외를 던진다")
        fun throwsWhenMissingAndApply() {
            every { command.findEntityByMemberIdAndJobSummaryId(1L, 100L) } returns null

            assertThatThrownBy {
                service.changeSaveType(1L, 100L, MemberJobSummarySaveType.APPLY)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("APPLY is set only when preparation records are written")
        }

        @Test
        @DisplayName("요약이 없고 UNSAVED 요청이면 아무 작업도 하지 않는다")
        fun ignoresUnsaveWhenMissing() {
            every { command.findEntityByMemberIdAndJobSummaryId(1L, 100L) } returns null

            service.changeSaveType(1L, 100L, MemberJobSummarySaveType.UNSAVED)

            verify(exactly = 0) { command.save(any()) }
            verify(exactly = 0) { jobSummaryCommand.findById(any()) }
        }
    }

    @Nested
    @DisplayName("cover letter")
    inner class CoverLetterTest {

        @Test
        @DisplayName("addCoverLetter: 요약이 없으면 IllegalArgumentException을 던진다")
        fun addCoverLetterThrowsWhenNotFound() {
            every { command.findEntityByMemberIdAndJobSummaryId(1L, 100L) } returns null

            assertThatThrownBy {
                service.addCoverLetter(1L, 100L, "q", "a", null)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("MemberJobSummary not found")
        }

        @Test
        @DisplayName("updateCoverLetter: 요약이 없으면 IllegalArgumentException을 던진다")
        fun updateCoverLetterThrowsWhenSummaryNotFound() {
            every { command.findEntityByMemberIdAndJobSummaryId(1L, 100L) } returns null

            assertThatThrownBy {
                service.updateCoverLetter(1L, 100L, 1L, "q", "a", 0)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("MemberJobSummary not found")
        }

        @Test
        @DisplayName("updateCoverLetter: 커버레터가 없으면 IllegalStateException을 던진다")
        fun updateCoverLetterThrowsWhenLetterNotFound() {
            val summary = createSummary()
            every { command.findEntityByMemberIdAndJobSummaryId(1L, 100L) } returns summary

            assertThatThrownBy {
                service.updateCoverLetter(1L, 100L, 999L, "q", "a", 0)
            }.isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("CoverLetter not found")
        }

        @Test
        @DisplayName("removeCoverLetter: 요약이 없으면 IllegalArgumentException을 던진다")
        fun removeCoverLetterThrowsWhenNotFound() {
            every { command.findEntityByMemberIdAndJobSummaryId(1L, 100L) } returns null

            assertThatThrownBy {
                service.removeCoverLetter(1L, 100L, 1L)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("MemberJobSummary not found")
        }
    }
}
