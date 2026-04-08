package com.hirelog.api.job.domain.model

import com.hirelog.api.job.domain.type.JdSummaryProcessingStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.UUID

class JdSummaryProcessingPostLlmFailedTest {

    @Test
    @DisplayName("POST_LLM_FAILED 상태에서도 COMPLETED로 전이할 수 있다")
    fun shouldCompleteFromPostLlmFailed() {
        val processing = JdSummaryProcessing.create(
            id = UUID.randomUUID(),
            brandName = "Toss",
            positionName = "Backend Engineer"
        )

        processing.markSummarizing(1L)
        processing.saveLlmResult("{}", "Toss", "Backend Engineer")
        processing.markPostLlmFailed("POST_LLM_FAILED", "db write failed")

        processing.markCompleted(99L)

        assertThat(processing.status).isEqualTo(JdSummaryProcessingStatus.COMPLETED)
        assertThat(processing.jobSummaryId).isEqualTo(99L)
        assertThat(processing.llmResultJson).isNull()
        assertThat(processing.errorCode).isNull()
        assertThat(processing.errorMessage).isNull()
    }
}

