package com.hirelog.api.company.application

import com.hirelog.api.common.utils.Normalizer
import com.hirelog.api.company.application.port.CompanyCandidateCommand
import com.hirelog.api.company.application.port.CompanyCommand
import com.hirelog.api.company.domain.CompanyCandidate
import com.hirelog.api.company.domain.Company
import com.hirelog.api.job.application.summary.port.JobSummaryCommand
import com.hirelog.api.job.domain.model.JobSummary
import io.mockk.*
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.*

@DisplayName("CompanyCandidateProcessingService 테스트")
class CompanyCandidateProcessingServiceTest {

    private lateinit var service: CompanyCandidateProcessingService
    private lateinit var candidateCommand: CompanyCandidateCommand
    private lateinit var companyCommand: CompanyCommand
    private lateinit var jobSummaryCommand: JobSummaryCommand

    @BeforeEach
    fun setUp() {
        candidateCommand = mockk()
        companyCommand = mockk()
        jobSummaryCommand = mockk()
        service = CompanyCandidateProcessingService(candidateCommand, companyCommand, jobSummaryCommand)
        mockkObject(Normalizer)
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(Normalizer)
    }

    @Nested
    @DisplayName("fetchAndMarkProcessing 메서드는")
    inner class FetchAndMarkProcessingTest {

        @Test
        @DisplayName("APPROVED 후보를 조회하고 PROCESSING 상태로 변경한다")
        fun shouldFetchAndMarkProcessing() {
            val candidate1 = mockk<CompanyCandidate>(relaxed = true)
            val candidate2 = mockk<CompanyCandidate>(relaxed = true)

            every { candidateCommand.findApprovedForUpdate(5) } returns listOf(candidate1, candidate2)

            val result = service.fetchAndMarkProcessing(5)

            verify { candidate1.markProcessing() }
            verify { candidate2.markProcessing() }
            assert(result.size == 2)
        }

        @Test
        @DisplayName("처리할 후보가 없으면 빈 목록을 반환한다")
        fun shouldReturnEmptyWhenNoCandidates() {
            every { candidateCommand.findApprovedForUpdate(5) } returns emptyList()

            val result = service.fetchAndMarkProcessing(5)

            assert(result.isEmpty())
        }
    }

    @Nested
    @DisplayName("process 메서드는")
    inner class ProcessTest {

        @Test
        @DisplayName("doProcess 성공 시 예외를 던지지 않는다")
        fun shouldSucceedWhenDoProcessSucceeds() {
            val candidate = mockk<CompanyCandidate>(relaxed = true)
            every { candidate.normalizedName } returns "toss"
            every { candidate.candidateName } returns "Toss"
            every { candidate.jdSummaryId } returns 1L

            val existingCompany = mockk<Company>(relaxed = true)
            val jobSummary = mockk<JobSummary>(relaxed = true)

            every { companyCommand.findByNormalizedName("toss") } returns existingCompany
            every { jobSummaryCommand.findById(1L) } returns jobSummary
            every { jobSummaryCommand.update(jobSummary) } just runs

            service.process(candidate)

            verify { candidate.complete() }
        }

        @Test
        @DisplayName("doProcess 실패 시 markFailed를 호출하고 예외를 재던진다")
        fun shouldCallMarkFailedAndRethrowOnError() {
            val candidate = mockk<CompanyCandidate>(relaxed = true)
            every { candidate.id } returns 1L
            every { candidate.normalizedName } returns "toss"
            every { candidate.candidateName } returns "Toss"
            every { candidate.jdSummaryId } returns 1L

            every { companyCommand.findByNormalizedName("toss") } throws RuntimeException("DB error")

            val failedCandidate = mockk<CompanyCandidate>(relaxed = true)
            every { candidateCommand.findByIdForUpdate(1L) } returns failedCandidate

            assertThatThrownBy {
                service.process(candidate)
            }.isInstanceOf(RuntimeException::class.java)
                .hasMessageContaining("DB error")

            verify { failedCandidate.fail() }
        }
    }

    @Nested
    @DisplayName("doProcess 메서드는")
    inner class DoProcessTest {

        @Test
        @DisplayName("Company가 이미 존재하면 재사용하고 후보를 완료 처리한다")
        fun shouldReuseExistingCompany() {
            val candidate = mockk<CompanyCandidate>(relaxed = true)
            every { candidate.normalizedName } returns "toss"
            every { candidate.candidateName } returns "Toss"
            every { candidate.jdSummaryId } returns 1L

            val existingCompany = mockk<Company>(relaxed = true)
            val jobSummary = mockk<JobSummary>(relaxed = true)

            every { companyCommand.findByNormalizedName("toss") } returns existingCompany
            every { jobSummaryCommand.findById(1L) } returns jobSummary
            every { jobSummaryCommand.update(jobSummary) } just runs

            service.doProcess(candidate)

            verify(exactly = 0) { companyCommand.save(any()) }
            verify { jobSummary.applyCompany(any(), any()) }
            verify { jobSummaryCommand.update(jobSummary) }
            verify { candidate.complete() }
        }

        @Test
        @DisplayName("Company가 없으면 신규 생성 후 후보를 완료 처리한다")
        fun shouldCreateNewCompanyWhenNotExists() {
            val candidate = mockk<CompanyCandidate>(relaxed = true)
            every { candidate.normalizedName } returns "toss"
            every { candidate.candidateName } returns "Toss"
            every { candidate.jdSummaryId } returns 1L

            val newCompany = mockk<Company>(relaxed = true)
            val jobSummary = mockk<JobSummary>(relaxed = true)

            every { Normalizer.normalizeCompany("Toss") } returns "toss"
            every { companyCommand.findByNormalizedName("toss") } returns null
            every { companyCommand.save(any()) } returns newCompany
            every { jobSummaryCommand.findById(1L) } returns jobSummary
            every { jobSummaryCommand.update(jobSummary) } just runs

            service.doProcess(candidate)

            verify(exactly = 1) { companyCommand.save(any()) }
            verify { candidate.complete() }
        }

        @Test
        @DisplayName("JobSummary가 없으면 예외를 던진다")
        fun shouldThrowWhenJobSummaryNotFound() {
            val candidate = mockk<CompanyCandidate>(relaxed = true)
            every { candidate.normalizedName } returns "toss"
            every { candidate.jdSummaryId } returns 999L

            val existingCompany = mockk<Company>(relaxed = true)

            every { companyCommand.findByNormalizedName("toss") } returns existingCompany
            every { jobSummaryCommand.findById(999L) } returns null

            assertThatThrownBy {
                service.doProcess(candidate)
            }.isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("JobSummary not found")
        }
    }

    @Nested
    @DisplayName("markFailed 메서드는")
    inner class MarkFailedTest {

        @Test
        @DisplayName("후보를 FAILED 상태로 변경한다")
        fun shouldMarkCandidateAsFailed() {
            val candidate = mockk<CompanyCandidate>(relaxed = true)
            every { candidateCommand.findByIdForUpdate(1L) } returns candidate

            service.markFailed(1L, RuntimeException("error"))

            verify { candidate.fail() }
        }

        @Test
        @DisplayName("후보를 찾지 못하면 아무 작업도 하지 않는다")
        fun shouldDoNothingWhenCandidateNotFound() {
            every { candidateCommand.findByIdForUpdate(999L) } returns null

            service.markFailed(999L, RuntimeException("error"))
        }
    }
}
