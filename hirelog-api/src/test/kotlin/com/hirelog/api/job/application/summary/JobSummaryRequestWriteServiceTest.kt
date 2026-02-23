package com.hirelog.api.job.application.summary

import com.hirelog.api.job.application.summary.port.JobSummaryRequestCommand
import com.hirelog.api.job.domain.model.JobSummaryRequest
import com.hirelog.api.job.domain.type.JobSummaryRequestStatus
import com.hirelog.api.relation.application.memberjobsummary.MemberJobSummaryWriteService
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*

@DisplayName("JobSummaryRequestWriteService 테스트")
class JobSummaryRequestWriteServiceTest {

    private lateinit var service: JobSummaryRequestWriteService
    private lateinit var jobSummaryRequestCommand: JobSummaryRequestCommand
    private lateinit var memberJobSummaryWriteService: MemberJobSummaryWriteService

    @BeforeEach
    fun setUp() {
        jobSummaryRequestCommand = mockk()
        memberJobSummaryWriteService = mockk(relaxed = true)
        service = JobSummaryRequestWriteService(jobSummaryRequestCommand, memberJobSummaryWriteService)
    }

    @Nested
    @DisplayName("createRequest 메서드는")
    inner class CreateRequestTest {

        @Test
        @DisplayName("요청을 생성하고 저장 후 반환한다")
        fun shouldCreateAndReturnRequest() {
            val savedRequest = mockk<JobSummaryRequest>(relaxed = true)
            every { jobSummaryRequestCommand.save(any()) } returns savedRequest

            val result = service.createRequest(1L, "req-uuid-001")

            assertThat(result).isEqualTo(savedRequest)
            verify(exactly = 1) { jobSummaryRequestCommand.save(any()) }
        }
    }

    @Nested
    @DisplayName("completeRequest 메서드는")
    inner class CompleteRequestTest {

        @Test
        @DisplayName("PENDING 요청이 있으면 완료 처리하고 memberId를 반환한다")
        fun shouldCompleteRequestAndReturnMemberId() {
            val request = mockk<JobSummaryRequest>(relaxed = true)
            every { request.memberId } returns 10L

            every {
                jobSummaryRequestCommand.findByRequestIdAndStatus("req-001", JobSummaryRequestStatus.PENDING)
            } returns request
            every { jobSummaryRequestCommand.save(request) } returns request

            val result = service.completeRequest(
                requestId = "req-001",
                jobSummaryId = 99L,
                brandName = "Toss",
                positionName = "Backend Engineer",
                brandPositionName = "Toss Backend",
                positionCategoryName = "Engineering"
            )

            assertThat(result).isEqualTo(10L)
            verify { request.complete(99L) }
            verify { jobSummaryRequestCommand.save(request) }
            verify { memberJobSummaryWriteService.save(any()) }
        }

        @Test
        @DisplayName("PENDING 요청이 없으면 null을 반환한다")
        fun shouldReturnNullWhenNoPendingRequest() {
            every {
                jobSummaryRequestCommand.findByRequestIdAndStatus("unknown-req", JobSummaryRequestStatus.PENDING)
            } returns null

            val result = service.completeRequest(
                requestId = "unknown-req",
                jobSummaryId = 99L,
                brandName = "Toss",
                positionName = "Backend",
                brandPositionName = "Toss Backend",
                positionCategoryName = "Engineering"
            )

            assertThat(result).isNull()
            verify(exactly = 0) { memberJobSummaryWriteService.save(any()) }
        }
    }

    @Nested
    @DisplayName("failRequest 메서드는")
    inner class FailRequestTest {

        @Test
        @DisplayName("PENDING 요청이 있으면 FAILED로 전이하고 memberId를 반환한다")
        fun shouldFailRequestAndReturnMemberId() {
            val request = mockk<JobSummaryRequest>(relaxed = true)
            every { request.memberId } returns 5L

            every {
                jobSummaryRequestCommand.findByRequestIdAndStatus("req-001", JobSummaryRequestStatus.PENDING)
            } returns request
            every { jobSummaryRequestCommand.save(request) } returns request

            val result = service.failRequest("req-001")

            assertThat(result).isEqualTo(5L)
            verify { request.markFailed() }
            verify { jobSummaryRequestCommand.save(request) }
        }

        @Test
        @DisplayName("PENDING 요청이 없으면 null을 반환한다")
        fun shouldReturnNullWhenNoPendingRequest() {
            every {
                jobSummaryRequestCommand.findByRequestIdAndStatus("no-req", JobSummaryRequestStatus.PENDING)
            } returns null

            val result = service.failRequest("no-req")

            assertThat(result).isNull()
            verify(exactly = 0) { jobSummaryRequestCommand.save(any()) }
        }
    }
}
