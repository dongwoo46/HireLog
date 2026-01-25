package com.hirelog.api.common.infra.redis.messaging

import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.data.domain.Range
import org.springframework.data.redis.connection.stream.*
import org.springframework.data.redis.core.StreamOperations
import org.springframework.data.redis.core.StringRedisTemplate
import java.time.Duration
import java.util.concurrent.CompletableFuture
import kotlin.test.assertEquals

@ExtendWith(MockKExtension::class)
class RedisStreamConsumerTest {

    @MockK
    lateinit var redisTemplate: StringRedisTemplate

    @MockK
    lateinit var streamOps: StreamOperations<String, String, String>

    private lateinit var consumer: RedisStreamConsumer

    private val testStreamKey = "test:stream"
    private val testGroup = "test-group"
    private val testConsumer = "test-consumer"

    @BeforeEach
    fun setUp() {
        every { redisTemplate.opsForStream<String, String>() } returns streamOps
        consumer = RedisStreamConsumer(redisTemplate)
    }

    @Nested
    @DisplayName("sweepPendingMessages 테스트")
    inner class SweepPendingMessagesTest {

        @Test
        @DisplayName("pending 메시지가 없으면 0 반환")
        fun `should return 0 when no pending messages`() {
            // given
            val pendingSummary = mockk<PendingMessagesSummary> {
                every { totalPendingMessages } returns 0L
            }

            every { streamOps.createGroup(any(), any<ReadOffset>(), any()) } returns "OK"
            every { streamOps.pending(testStreamKey, testGroup) } returns pendingSummary

            // when
            val result = consumer.sweepPendingMessages(
                streamKey = testStreamKey,
                group = testGroup,
                consumer = testConsumer
            ) { _, _ -> CompletableFuture.completedFuture(null) }

            // then
            assertEquals(0, result)
        }

        @Test
        @DisplayName("pending 메시지가 있지만 idle time이 부족하면 처리하지 않음")
        fun `should skip messages with insufficient idle time`() {
            // given
            val pendingSummary = mockk<PendingMessagesSummary> {
                every { totalPendingMessages } returns 1L
            }

            val pendingMessage = mockk<PendingMessage> {
                every { idAsString } returns "1234567890-0"
                every { elapsedTimeSinceLastDelivery } returns Duration.ofMillis(30_000L)  // 30초 (최소 60초 필요)
            }

            val pendingMessages = mockk<PendingMessages> {
                every { isEmpty } returns false
                every { iterator() } returns mutableListOf(pendingMessage).iterator()
            }

            every { streamOps.createGroup(any(), any<ReadOffset>(), any()) } returns "OK"
            every { streamOps.pending(testStreamKey, testGroup) } returns pendingSummary
            every {
                streamOps.pending(
                    testStreamKey,
                    Consumer.from(testGroup, testConsumer),
                    any<Range<String>>(),
                    any<Long>()
                )
            } returns pendingMessages

            // when
            val result = consumer.sweepPendingMessages(
                streamKey = testStreamKey,
                group = testGroup,
                consumer = testConsumer
            ) { _, _ -> CompletableFuture.completedFuture(null) }

            // then
            assertEquals(0, result)
            verify(exactly = 0) { streamOps.claim(any(), any(), any(), any<Duration>(), any()) }
        }

        @Test
        @DisplayName("idle time이 충분한 pending 메시지는 claim하여 처리")
        fun `should claim and process messages with sufficient idle time`() {
            // given
            val pendingSummary = mockk<PendingMessagesSummary> {
                every { totalPendingMessages } returns 1L
            }

            val pendingMessage = mockk<PendingMessage> {
                every { idAsString } returns "1234567890-0"
                every { elapsedTimeSinceLastDelivery } returns Duration.ofMillis(120_000L)  // 2분 (최소 60초 필요)
                every { totalDeliveryCount } returns 2L
            }

            val pendingMessages = mockk<PendingMessages> {
                every { isEmpty } returns false
                every { iterator() } returns mutableListOf(pendingMessage).iterator()
            }

            val recordId = RecordId.of("1234567890-0")
            val claimedRecord = mockk<MapRecord<String, String, String>> {
                every { id } returns recordId
                every { value } returns mapOf("data" to "test")
            }

            every { streamOps.createGroup(any(), any<ReadOffset>(), any()) } returns "OK"
            every { streamOps.pending(testStreamKey, testGroup) } returns pendingSummary
            every {
                streamOps.pending(
                    testStreamKey,
                    Consumer.from(testGroup, testConsumer),
                    any<Range<String>>(),
                    any<Long>()
                )
            } returns pendingMessages
            every {
                streamOps.claim(
                    testStreamKey,
                    testGroup,
                    testConsumer,
                    any<Duration>(),
                    RecordId.of("1234567890-0")
                )
            } returns listOf(claimedRecord)
            every { streamOps.acknowledge(testStreamKey, testGroup, recordId) } returns 1L

            var handlerCalled = false

            // when
            val result = consumer.sweepPendingMessages(
                streamKey = testStreamKey,
                group = testGroup,
                consumer = testConsumer
            ) { rid, msg ->
                handlerCalled = true
                assertEquals("1234567890-0", rid)
                assertEquals("test", msg["data"])
                CompletableFuture.completedFuture(null)
            }

            // then
            assertEquals(1, result)
            assertEquals(true, handlerCalled)
            verify { streamOps.acknowledge(testStreamKey, testGroup, recordId) }
        }

        @Test
        @DisplayName("handler 실패 시 ACK하지 않음")
        fun `should not acknowledge when handler fails`() {
            // given
            val pendingSummary = mockk<PendingMessagesSummary> {
                every { totalPendingMessages } returns 1L
            }

            val pendingMessage = mockk<PendingMessage> {
                every { idAsString } returns "1234567890-0"
                every { elapsedTimeSinceLastDelivery } returns Duration.ofMillis(120_000L)
                every { totalDeliveryCount } returns 1L
            }

            val pendingMessages = mockk<PendingMessages> {
                every { isEmpty } returns false
                every { iterator() } returns mutableListOf(pendingMessage).iterator()
            }

            val recordId = RecordId.of("1234567890-0")
            val claimedRecord = mockk<MapRecord<String, String, String>> {
                every { id } returns recordId
                every { value } returns mapOf("data" to "test")
            }

            every { streamOps.createGroup(any(), any<ReadOffset>(), any()) } returns "OK"
            every { streamOps.pending(testStreamKey, testGroup) } returns pendingSummary
            every {
                streamOps.pending(
                    testStreamKey,
                    Consumer.from(testGroup, testConsumer),
                    any<Range<String>>(),
                    any<Long>()
                )
            } returns pendingMessages
            every {
                streamOps.claim(
                    testStreamKey,
                    testGroup,
                    testConsumer,
                    any<Duration>(),
                    RecordId.of("1234567890-0")
                )
            } returns listOf(claimedRecord)

            // when
            val result = consumer.sweepPendingMessages(
                streamKey = testStreamKey,
                group = testGroup,
                consumer = testConsumer
            ) { _, _ ->
                CompletableFuture.failedFuture(RuntimeException("Processing failed"))
            }

            // then
            assertEquals(1, result)  // 처리 시도는 했으므로 count 증가
            verify(exactly = 0) { streamOps.acknowledge(any(), any(), any<RecordId>()) }
        }

        @Test
        @DisplayName("Consumer Group이 없으면 자동 생성")
        fun `should create consumer group if not exists`() {
            // given
            val pendingSummary = mockk<PendingMessagesSummary> {
                every { totalPendingMessages } returns 0L
            }

            every { streamOps.createGroup(testStreamKey, ReadOffset.from("0-0"), testGroup) } returns "OK"
            every { streamOps.pending(testStreamKey, testGroup) } returns pendingSummary

            // when
            consumer.sweepPendingMessages(
                streamKey = testStreamKey,
                group = testGroup,
                consumer = testConsumer
            ) { _, _ -> CompletableFuture.completedFuture(null) }

            // then
            verify { streamOps.createGroup(testStreamKey, ReadOffset.from("0-0"), testGroup) }
        }

        @Test
        @DisplayName("Consumer Group이 이미 존재하면 BUSYGROUP 에러 무시")
        fun `should ignore BUSYGROUP error when group already exists`() {
            // given
            val pendingSummary = mockk<PendingMessagesSummary> {
                every { totalPendingMessages } returns 0L
            }

            every {
                streamOps.createGroup(testStreamKey, ReadOffset.from("0-0"), testGroup)
            } throws RuntimeException("BUSYGROUP Consumer Group name already exists")
            every { streamOps.pending(testStreamKey, testGroup) } returns pendingSummary

            // when - should not throw
            val result = consumer.sweepPendingMessages(
                streamKey = testStreamKey,
                group = testGroup,
                consumer = testConsumer
            ) { _, _ -> CompletableFuture.completedFuture(null) }

            // then
            assertEquals(0, result)
        }
    }
}
