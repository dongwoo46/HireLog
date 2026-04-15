package com.hirelog.api.job.infra.kafka.consumer

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.hirelog.api.job.application.summary.payload.JobSummarySearchPayload
import com.hirelog.api.job.application.summary.port.JobSummaryEmbedding
import com.hirelog.api.job.infra.persistence.opensearch.JobSummaryOpenSearchAdapter
import io.mockk.*
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.kafka.support.Acknowledgment

/**
 * JobSummaryIndexingConsumer 단위 테스트
 *
 * 검증 대상:
 * - 임베딩 성공/실패 시 OpenSearch 인덱싱 행동 (그레이스풀 처리)
 *
 * 테스트 전략 (Khorikov 고전파):
 * - openSearchAdapter.index() = Mock (커맨드, 검증 대상)
 * - embeddingPort.embed()     = Stub (쿼리, 검증 안 함)
 * - ObjectMapper              = 실제 객체 사용 (인-프로세스)
 * - handleIndex()는 private → consume() 공개 인터페이스를 통해 테스트
 */
@DisplayName("JobSummaryIndexingConsumer 테스트")
class JobSummaryIndexingConsumerTest {

    private val openSearchAdapter: JobSummaryOpenSearchAdapter = mockk()
    private val embeddingPort: JobSummaryEmbedding = mockk()
    private val objectMapper = jacksonObjectMapper()

    private lateinit var consumer: JobSummaryIndexingConsumer

    @BeforeEach
    fun setUp() {
        consumer = JobSummaryIndexingConsumer(openSearchAdapter, embeddingPort, objectMapper)
    }

    /**
     * CREATED 이벤트 최소 유효 payload
     * 임베딩 로직 테스트에 필요한 필드만 포함
     */
    private val sampleOutboxPayloadJson = """
        {
          "id": 1,
          "jobSnapshotId": 10,
          "brandId": 100,
          "brandName": "토스",
          "companyId": null,
          "companyName": null,
          "positionId": 300,
          "positionName": "백엔드 엔지니어",
          "brandPositionId": null,
          "brandPositionName": null,
          "positionCategoryId": 500,
          "positionCategoryName": "개발",
          "careerType": "EXPERIENCED",
          "careerYears": null,
          "summaryText": "요약 텍스트",
          "responsibilities": "핵심 업무 내용",
          "requiredQualifications": "Kotlin 3년 이상",
          "preferredQualifications": null,
          "techStack": null,
          "techStackParsed": null,
          "recruitmentProcess": null,
          "idealCandidate": null,
          "mustHaveSignals": null,
          "preparationFocus": null,
          "transferableStrengthsAndGapPlan": null,
          "proofPointsAndMetrics": null,
          "storyAngles": null,
          "keyChallenges": null,
          "technicalContext": null,
          "questionsToAsk": null,
          "considerations": null,
          "createdAt": "2026-04-13T12:00:00"
        }
    """.trimIndent()

    private fun buildCreatedRecord(payload: String): ConsumerRecord<String, String> {
        val record = ConsumerRecord("outbox.public.job_summary_outbox_events", 0, 0L, "1", payload)
        record.headers().add("eventType", "CREATED".toByteArray(Charsets.UTF_8))
        return record
    }

    @Nested
    @DisplayName("CREATED 이벤트 임베딩 처리는")
    inner class CreatedEventEmbeddingTest {

        @Test
        @DisplayName("임베딩 성공 시 벡터와 함께 OpenSearch에 인덱싱한다")
        fun shouldIndexWithEmbeddingVectorOnSuccess() {
            // Arrange
            val expectedVector = listOf(0.1f, 0.2f, 0.3f)
            val record = buildCreatedRecord(sampleOutboxPayloadJson)
            val acknowledgment: Acknowledgment = mockk(relaxed = true)
            val indexedPayloadSlot = slot<JobSummarySearchPayload>()

            every { embeddingPort.embed(any()) } returns expectedVector
            every { openSearchAdapter.index(capture(indexedPayloadSlot)) } just Runs

            // Act
            consumer.consume(record, acknowledgment)

            // Assert
            assertThat(indexedPayloadSlot.captured.embeddingVector).isEqualTo(expectedVector)
        }

        @Test
        @DisplayName("임베딩 서버 장애 시 null 벡터로 인덱싱하고 Kafka 메시지를 정상 커밋한다")
        fun shouldIndexWithNullVectorAndCommitMessageOnEmbeddingFailure() {
            // Arrange
            val record = buildCreatedRecord(sampleOutboxPayloadJson)
            val acknowledgment: Acknowledgment = mockk(relaxed = true)
            val indexedPayloadSlot = slot<JobSummarySearchPayload>()

            every { embeddingPort.embed(any()) } throws RuntimeException("임베딩 서버 연결 실패")
            every { openSearchAdapter.index(capture(indexedPayloadSlot)) } just Runs

            // Act — 예외가 전파되지 않아야 한다 (BM25 검색은 계속 동작)
            consumer.consume(record, acknowledgment)

            // Assert
            assertThat(indexedPayloadSlot.captured.embeddingVector).isNull()
            // 인덱싱 자체는 수행되었음을 검증 (임베딩 실패가 인덱싱을 막아선 안 된다)
            verify(exactly = 1) { openSearchAdapter.index(any()) }
            // Kafka offset 커밋 — 임베딩 실패를 재처리하면 안 됨
            verify(exactly = 1) { acknowledgment.acknowledge() }
        }
    }
}