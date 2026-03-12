package com.hirelog.api.job.application.snapshot

import com.hirelog.api.common.exception.EntityAlreadyExistsException
import com.hirelog.api.job.application.snapshot.command.JobSnapshotCreateCommand
import com.hirelog.api.job.application.snapshot.port.JobSnapshotCommand
import com.hirelog.api.job.domain.model.JobSnapshot
import com.hirelog.api.job.domain.type.JobSourceType
import com.hirelog.api.job.domain.type.RecruitmentPeriodType
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.*
import org.springframework.dao.DataIntegrityViolationException

@DisplayName("JobSnapshotWriteService 테스트")
class JobSnapshotWriteServiceTest {

    private lateinit var service: JobSnapshotWriteService
    private lateinit var snapshotCommand: JobSnapshotCommand

    private val command = JobSnapshotCreateCommand(
        sourceType = JobSourceType.TEXT,
        sourceUrl = null,
        canonicalMap = mapOf("responsibilities" to listOf("개발")),
        coreText = "개발 업무",
        recruitmentPeriodType = RecruitmentPeriodType.UNKNOWN,
        openedDate = null,
        closedDate = null,
        canonicalHash = "hash-abc-123",
        simHash = 123456789L
    )

    @BeforeEach
    fun setUp() {
        snapshotCommand = mockk(relaxed = true)
        service = JobSnapshotWriteService(snapshotCommand)
    }

    @Nested
    @DisplayName("record 메서드는")
    inner class RecordTest {

        @Test
        @DisplayName("정상 흐름에서 snapshotId를 반환한다")
        fun shouldReturnSnapshotId() {
            every { snapshotCommand.record(any()) } returns 100L

            val id = service.record(command)

            assertThat(id).isEqualTo(100L)
            verify { snapshotCommand.record(any()) }
        }

        @Test
        @DisplayName("DataIntegrityViolationException → EntityAlreadyExistsException으로 변환된다")
        fun shouldConvertDataIntegrityException() {
            every { snapshotCommand.record(any()) } throws
                DataIntegrityViolationException("unique constraint violated")

            assertThatThrownBy { service.record(command) }
                .isInstanceOf(EntityAlreadyExistsException::class.java)
                .hasMessageContaining("JobSnapshot")
        }
    }

    @Nested
    @DisplayName("attachBrandAndPosition 메서드는")
    inner class AttachBrandAndPositionTest {

        @Test
        @DisplayName("정상 흐름에서 snapshot에 brand/position을 연결하고 update한다")
        fun shouldAttachBrandAndPosition() {
            val snapshot = mockk<JobSnapshot>(relaxed = true)
            every { snapshotCommand.findById(10L) } returns snapshot

            service.attachBrandAndPosition(snapshotId = 10L, brandId = 1L, positionId = 2L)

            verify { snapshot.attachAnalysisResult(brandId = 1L, positionId = 2L) }
            verify { snapshotCommand.update(snapshot) }
        }

        @Test
        @DisplayName("Snapshot이 없으면 IllegalStateException을 던진다")
        fun shouldThrowWhenSnapshotNotFound() {
            every { snapshotCommand.findById(999L) } returns null

            assertThatThrownBy {
                service.attachBrandAndPosition(snapshotId = 999L, brandId = 1L, positionId = 2L)
            }.isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("Snapshot not found")
        }
    }
}
