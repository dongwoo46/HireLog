package com.hirelog.api.job.application.intake

import com.fasterxml.jackson.databind.ObjectMapper
import com.hirelog.api.common.application.outbox.OutboxEventWriteService
import com.hirelog.api.common.infra.storage.FileStorageService
import com.hirelog.api.job.application.summary.JobSummaryRequestWriteService
import com.hirelog.api.job.application.summary.port.JobSummaryQuery
import com.hirelog.api.job.application.summary.view.JobSummaryView
import com.hirelog.api.job.domain.type.CareerType
import com.hirelog.api.job.domain.type.JobPlatformType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockMultipartFile

@DisplayName("JdIntakeService 테스트")
class JdIntakeServiceTest {

    private lateinit var service: JdIntakeService
    private lateinit var fileStorageService: FileStorageService
    private lateinit var jobSummaryRequestWriteService: JobSummaryRequestWriteService
    private lateinit var outboxEventWriteService: OutboxEventWriteService
    private lateinit var jobSummaryQuery: JobSummaryQuery
    private val objectMapper = ObjectMapper()

    @BeforeEach
    fun setUp() {
        fileStorageService = mockk()
        jobSummaryRequestWriteService = mockk(relaxed = true)
        outboxEventWriteService = mockk(relaxed = true)
        jobSummaryQuery = mockk()
        service = JdIntakeService(
            fileStorageService,
            jobSummaryRequestWriteService,
            outboxEventWriteService,
            jobSummaryQuery,
            objectMapper
        )
    }

    @Nested
    @DisplayName("requestText 메서드는")
    inner class RequestTextTest {

        @Test
        @DisplayName("정상 입력이면 requestId를 반환하고 Outbox 이벤트를 저장한다")
        fun shouldReturnRequestIdAndAppendOutbox() {
            val requestId = service.requestText(
                memberId = 1L,
                brandName = "Toss",
                brandPositionName = "Backend Engineer",
                text = "JD 내용입니다."
            )

            assertThat(requestId).isNotBlank()
            verify { jobSummaryRequestWriteService.createRequest(1L, requestId) }
            verify { outboxEventWriteService.append(any()) }
        }

        @Test
        @DisplayName("brandName이 빈 값이면 예외를 던진다")
        fun shouldThrowWhenBrandNameBlank() {
            assertThatThrownBy {
                service.requestText(1L, "", "Backend Engineer", "JD 내용입니다.")
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("brandName")
        }

        @Test
        @DisplayName("brandPositionName이 빈 값이면 예외를 던진다")
        fun shouldThrowWhenPositionNameBlank() {
            assertThatThrownBy {
                service.requestText(1L, "Toss", "", "JD 내용입니다.")
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("positionName")
        }

        @Test
        @DisplayName("text가 빈 값이면 예외를 던진다")
        fun shouldThrowWhenTextBlank() {
            assertThatThrownBy {
                service.requestText(1L, "Toss", "Backend Engineer", "")
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("text")
        }
    }

    @Nested
    @DisplayName("requestOcr 메서드는")
    inner class RequestOcrTest {

        @Test
        @DisplayName("정상 입력이면 requestId를 반환하고 이미지를 저장한다")
        fun shouldSaveImagesAndReturnRequestId() {
            val imageFile = MockMultipartFile("file", "test.png", "image/png", byteArrayOf(1, 2, 3))
            val savedPaths = listOf("/storage/ocr/test.png")

            every { fileStorageService.saveImages(listOf(imageFile), "ocr") } returns savedPaths

            val requestId = service.requestOcr(
                memberId = 1L,
                brandName = "Toss",
                brandPositionName = "Backend Engineer",
                imageFiles = listOf(imageFile)
            )

            assertThat(requestId).isNotBlank()
            verify { fileStorageService.saveImages(listOf(imageFile), "ocr") }
            verify { jobSummaryRequestWriteService.createRequest(1L, requestId) }
            verify { outboxEventWriteService.append(any()) }
        }

        @Test
        @DisplayName("이미지 목록이 비어있으면 예외를 던진다")
        fun shouldThrowWhenImageFilesEmpty() {
            assertThatThrownBy {
                service.requestOcr(1L, "Toss", "Backend Engineer", emptyList())
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("imageFiles")
        }

        @Test
        @DisplayName("brandName이 빈 값이면 예외를 던진다")
        fun shouldThrowWhenBrandNameBlank() {
            val imageFile = MockMultipartFile("file", "test.png", "image/png", byteArrayOf(1, 2, 3))

            assertThatThrownBy {
                service.requestOcr(1L, "", "Backend Engineer", listOf(imageFile))
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("brandName")
        }
    }

    @Nested
    @DisplayName("requestUrl 메서드는")
    inner class RequestUrlTest {

        private fun existingView() = JobSummaryView(
            summaryId = 1L,
            snapshotId = 10L,
            brandId = 1L,
            brandName = "Toss",
            positionId = 2L,
            positionName = "Backend",
            brandPositionId = 3L,
            positionCategoryId = 4L,
            positionCategoryName = "Engineering",
            careerType = CareerType.EXPERIENCED,
            careerYears = null,
            summary = "요약",
            responsibilities = "업무",
            requiredQualifications = "자격",
            preferredQualifications = null,
            techStack = "Kotlin"
        )

        @Test
        @DisplayName("신규 URL이면 NewRequest를 반환하고 Outbox 이벤트를 저장한다")
        fun shouldReturnNewRequestForNewUrl() {
            every { jobSummaryQuery.findBySourceUrl("https://www.toss.im/careers/123") } returns null

            val result = service.requestUrl(
                memberId = 1L,
                brandName = "Toss",
                brandPositionName = "Backend Engineer",
                url = "https://www.toss.im/careers/123",
                platform = JobPlatformType.OTHER
            )

            assertThat(result).isInstanceOf(UrlIntakeResult.NewRequest::class.java)
            assertThat((result as UrlIntakeResult.NewRequest).requestId).isNotBlank()
            verify { jobSummaryRequestWriteService.createRequest(1L, result.requestId) }
            verify { outboxEventWriteService.append(any()) }
        }

        @Test
        @DisplayName("동일 URL의 JobSummary가 이미 있으면 Duplicate를 반환하고 Outbox를 저장하지 않는다")
        fun shouldReturnDuplicateWhenUrlAlreadyExists() {
            every { jobSummaryQuery.findBySourceUrl("https://www.toss.im/careers/123") } returns existingView()

            val result = service.requestUrl(
                memberId = 1L,
                brandName = "Toss",
                brandPositionName = "Backend Engineer",
                url = "https://www.toss.im/careers/123",
                platform = JobPlatformType.OTHER
            )

            assertThat(result).isInstanceOf(UrlIntakeResult.Duplicate::class.java)
            assertThat((result as UrlIntakeResult.Duplicate).existing.summaryId).isEqualTo(1L)
            verify(exactly = 0) { outboxEventWriteService.append(any()) }
            verify(exactly = 0) { jobSummaryRequestWriteService.createRequest(any(), any()) }
        }

        @Test
        @DisplayName("잘못된 URL 형식이면 예외를 던진다")
        fun shouldThrowWhenInvalidUrl() {
            assertThatThrownBy {
                service.requestUrl(1L, "Toss", "Backend Engineer", "not-a-url", JobPlatformType.OTHER)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("Invalid URL")
        }

        @Test
        @DisplayName("http/https가 아닌 scheme은 거부한다")
        fun shouldRejectNonHttpScheme() {
            assertThatThrownBy {
                service.requestUrl(1L, "Toss", "Backend Engineer", "ftp://example.com/jd", JobPlatformType.OTHER)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("Invalid URL")
        }

        @Test
        @DisplayName("brandName이 빈 값이면 예외를 던진다")
        fun shouldThrowWhenBrandNameBlank() {
            assertThatThrownBy {
                service.requestUrl(1L, "", "Backend Engineer", "https://example.com", JobPlatformType.OTHER)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("brandName")
        }
    }
}
