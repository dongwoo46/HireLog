package com.hirelog.api.job.domain.model

import com.hirelog.api.job.domain.type.JdSummaryProcessingStatus
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.*
import java.util.UUID

@DisplayName("JdSummaryProcessing 도메인 테스트")
class JdSummaryProcessingTest {

    private fun newProcessing(): JdSummaryProcessing =
        JdSummaryProcessing.create(UUID.randomUUID())

    @Nested
    @DisplayName("create 팩토리는")
    inner class CreateTest {

        @Test
        @DisplayName("초기 상태는 RECEIVED이다")
        fun shouldCreateWithReceivedStatus() {
            val processing = newProcessing()
            assertThat(processing.status).isEqualTo(JdSummaryProcessingStatus.RECEIVED)
            assertThat(processing.jobSnapshotId).isNull()
            assertThat(processing.errorCode).isNull()
        }
    }

    @Nested
    @DisplayName("markDuplicate는")
    inner class MarkDuplicateTest {

        @Test
        @DisplayName("RECEIVED → DUPLICATE로 전이하고 reason을 설정한다")
        fun shouldTransitionToDuplicate() {
            val processing = newProcessing()
            processing.markDuplicate("HASH")

            assertThat(processing.status).isEqualTo(JdSummaryProcessingStatus.DUPLICATE)
            assertThat(processing.duplicateReason).isEqualTo("HASH")
            assertThat(processing.errorCode).isNull()
        }

        @Test
        @DisplayName("RECEIVED 이외 상태에서 호출하면 예외를 던진다")
        fun shouldThrowWhenNotReceived() {
            val processing = newProcessing()
            processing.markSummarizing(1L)

            assertThatThrownBy { processing.markDuplicate("HASH") }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("SUMMARIZING")
        }
    }

    @Nested
    @DisplayName("markSummarizing는")
    inner class MarkSummarizingTest {

        @Test
        @DisplayName("RECEIVED → SUMMARIZING으로 전이하고 snapshotId를 설정한다")
        fun shouldTransitionToSummarizing() {
            val processing = newProcessing()
            processing.markSummarizing(10L)

            assertThat(processing.status).isEqualTo(JdSummaryProcessingStatus.SUMMARIZING)
            assertThat(processing.jobSnapshotId).isEqualTo(10L)
        }

        @Test
        @DisplayName("snapshotId가 0 이하이면 예외를 던진다")
        fun shouldThrowWhenInvalidSnapshotId() {
            val processing = newProcessing()

            assertThatThrownBy { processing.markSummarizing(0L) }
                .isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        @DisplayName("RECEIVED 이외 상태에서 호출하면 예외를 던진다")
        fun shouldThrowWhenNotReceived() {
            val processing = newProcessing()
            processing.markDuplicate("HASH")

            assertThatThrownBy { processing.markSummarizing(1L) }
                .isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    @Nested
    @DisplayName("saveLlmResult는")
    inner class SaveLlmResultTest {

        @Test
        @DisplayName("SUMMARIZING 상태에서 LLM 결과를 저장한다")
        fun shouldSaveLlmResult() {
            val processing = newProcessing()
            processing.markSummarizing(1L)
            processing.saveLlmResult("{}", "Toss", "Backend Engineer")

            assertThat(processing.llmResultJson).isEqualTo("{}")
            assertThat(processing.commandBrandName).isEqualTo("Toss")
            assertThat(processing.commandPositionName).isEqualTo("Backend Engineer")
        }

        @Test
        @DisplayName("blank llmResultJson이면 예외를 던진다")
        fun shouldThrowWhenBlankJson() {
            val processing = newProcessing()
            processing.markSummarizing(1L)

            assertThatThrownBy { processing.saveLlmResult("  ", "Toss", "Backend") }
                .isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        @DisplayName("SUMMARIZING 이외 상태에서 호출하면 예외를 던진다")
        fun shouldThrowWhenNotSummarizing() {
            val processing = newProcessing()

            assertThatThrownBy { processing.saveLlmResult("{}", "Toss", "Backend") }
                .isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    @Nested
    @DisplayName("markCompleted는")
    inner class MarkCompletedTest {

        @Test
        @DisplayName("SUMMARIZING → COMPLETED로 전이하고 임시 데이터를 초기화한다")
        fun shouldTransitionToCompleted() {
            val processing = newProcessing()
            processing.markSummarizing(1L)
            processing.saveLlmResult("{}", "Toss", "Backend")
            processing.markCompleted(50L)

            assertThat(processing.status).isEqualTo(JdSummaryProcessingStatus.COMPLETED)
            assertThat(processing.jobSummaryId).isEqualTo(50L)
            assertThat(processing.llmResultJson).isNull()
            assertThat(processing.commandBrandName).isNull()
            assertThat(processing.commandPositionName).isNull()
            assertThat(processing.errorCode).isNull()
        }

        @Test
        @DisplayName("summaryId가 0 이하이면 예외를 던진다")
        fun shouldThrowWhenInvalidSummaryId() {
            val processing = newProcessing()
            processing.markSummarizing(1L)

            assertThatThrownBy { processing.markCompleted(0L) }
                .isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        @DisplayName("SUMMARIZING 이외 상태에서 호출하면 예외를 던진다")
        fun shouldThrowWhenNotSummarizing() {
            val processing = newProcessing()

            assertThatThrownBy { processing.markCompleted(1L) }
                .isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    @Nested
    @DisplayName("markFailed는")
    inner class MarkFailedTest {

        @Test
        @DisplayName("RECEIVED 상태에서 FAILED로 전이한다")
        fun shouldTransitionFromReceived() {
            val processing = newProcessing()
            processing.markFailed("LLM_CALL_FAILED", "API 오류")

            assertThat(processing.status).isEqualTo(JdSummaryProcessingStatus.FAILED)
            assertThat(processing.errorCode).isEqualTo("LLM_CALL_FAILED")
            assertThat(processing.duplicateReason).isNull()
        }

        @Test
        @DisplayName("SUMMARIZING 상태에서 FAILED로 전이한다")
        fun shouldTransitionFromSummarizing() {
            val processing = newProcessing()
            processing.markSummarizing(1L)
            processing.markFailed("LLM_TIMEOUT", "타임아웃")

            assertThat(processing.status).isEqualTo(JdSummaryProcessingStatus.FAILED)
            assertThat(processing.errorCode).isEqualTo("LLM_TIMEOUT")
        }

        @Test
        @DisplayName("errorMessage가 1000자를 초과하면 잘라낸다")
        fun shouldTruncateErrorMessage() {
            val processing = newProcessing()
            processing.markFailed("ERR", "a".repeat(1500))

            assertThat(processing.errorMessage!!.length).isEqualTo(1000)
        }

        @Test
        @DisplayName("DUPLICATE 상태에서 호출하면 예외를 던진다")
        fun shouldThrowWhenDuplicate() {
            val processing = newProcessing()
            processing.markDuplicate("HASH")

            assertThatThrownBy { processing.markFailed("ERR", "msg") }
                .isInstanceOf(IllegalArgumentException::class.java)
        }
    }
}
