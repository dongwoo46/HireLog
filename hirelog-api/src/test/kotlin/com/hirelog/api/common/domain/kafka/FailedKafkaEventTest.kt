package com.hirelog.api.common.domain.kafka

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

@DisplayName("FailedKafkaEvent 도메인 테스트")
class FailedKafkaEventTest {

    @Nested
    @DisplayName("create 메서드는")
    inner class CreateTest {

        @Test
        @DisplayName("실패한 Kafka 이벤트를 생성한다")
        fun shouldCreateFailedKafkaEvent() {
            // given
            val topic = "user.created"
            val partitionNumber = 0
            val offsetNumber = 12345L
            val recordKey = "user:123"
            val recordValue = """{"userId":123,"name":"test"}"""
            val consumerGroup = "user-service-group"
            val exception = RuntimeException("Processing failed")
            val retryCount = 3

            // when
            val event = FailedKafkaEvent.create(
                topic = topic,
                partitionNumber = partitionNumber,
                offsetNumber = offsetNumber,
                recordKey = recordKey,
                recordValue = recordValue,
                consumerGroup = consumerGroup,
                exception = exception,
                retryCount = retryCount
            )

            // then
            assertThat(event.topic).isEqualTo(topic)
            assertThat(event.partitionNumber).isEqualTo(partitionNumber)
            assertThat(event.offsetNumber).isEqualTo(offsetNumber)
            assertThat(event.recordKey).isEqualTo(recordKey)
            assertThat(event.recordValue).isEqualTo(recordValue)
            assertThat(event.consumerGroup).isEqualTo(consumerGroup)
            assertThat(event.exceptionClass).isEqualTo("java.lang.RuntimeException")
            assertThat(event.exceptionMessage).isEqualTo("Processing failed")
            assertThat(event.stackTrace).isNotNull()
            assertThat(event.retryCount).isEqualTo(retryCount)
            assertThat(event.status).isEqualTo(FailedEventStatus.FAILED)
            assertThat(event.failedAt).isNotNull()
            assertThat(event.reprocessedAt).isNull()
        }

        @Test
        @DisplayName("recordKey가 null인 경우도 처리할 수 있다")
        fun shouldCreateWithNullRecordKey() {
            // given
            val exception = IllegalArgumentException("Invalid data")

            // when
            val event = FailedKafkaEvent.create(
                topic = "test.topic",
                partitionNumber = 0,
                offsetNumber = 100L,
                recordKey = null,
                recordValue = "test value",
                consumerGroup = "test-group",
                exception = exception,
                retryCount = 1
            )

            // then
            assertThat(event.recordKey).isNull()
        }

        @Test
        @DisplayName("recordValue가 null인 경우도 처리할 수 있다")
        fun shouldCreateWithNullRecordValue() {
            // given
            val exception = NullPointerException("Null value")

            // when
            val event = FailedKafkaEvent.create(
                topic = "test.topic",
                partitionNumber = 0,
                offsetNumber = 100L,
                recordKey = "key",
                recordValue = null,
                consumerGroup = "test-group",
                exception = exception,
                retryCount = 1
            )

            // then
            assertThat(event.recordValue).isNull()
        }

        @Test
        @DisplayName("recordValue가 10KB를 초과하면 잘라낸다")
        fun shouldTruncateRecordValueOver10KB() {
            // given
            val largeValue = "A".repeat(15000) // 15KB
            val exception = RuntimeException("Error")

            // when
            val event = FailedKafkaEvent.create(
                topic = "test.topic",
                partitionNumber = 0,
                offsetNumber = 100L,
                recordKey = "key",
                recordValue = largeValue,
                consumerGroup = "test-group",
                exception = exception,
                retryCount = 1
            )

            // then
            assertThat(event.recordValue).hasSize(10000)
            assertThat(event.recordValue).isEqualTo("A".repeat(10000))
        }

        @Test
        @DisplayName("exceptionMessage가 2000자를 초과하면 잘라낸다")
        fun shouldTruncateExceptionMessageOver2000() {
            // given
            val longMessage = "Error: " + "X".repeat(3000)
            val exception = RuntimeException(longMessage)

            // when
            val event = FailedKafkaEvent.create(
                topic = "test.topic",
                partitionNumber = 0,
                offsetNumber = 100L,
                recordKey = "key",
                recordValue = "value",
                consumerGroup = "test-group",
                exception = exception,
                retryCount = 1
            )

            // then
            assertThat(event.exceptionMessage).hasSize(2000)
        }

        @Test
        @DisplayName("stackTrace가 5000자를 초과하면 잘라낸다")
        fun shouldTruncateStackTraceOver5000() {
            // given
            val exception = RuntimeException("Error with deep stack")

            // when
            val event = FailedKafkaEvent.create(
                topic = "test.topic",
                partitionNumber = 0,
                offsetNumber = 100L,
                recordKey = "key",
                recordValue = "value",
                consumerGroup = "test-group",
                exception = exception,
                retryCount = 1
            )

            // then
            assertThat(event.stackTrace).isNotNull()
            assertThat(event.stackTrace!!.length).isLessThanOrEqualTo(5000)
        }

        @Test
        @DisplayName("다양한 예외 타입을 처리할 수 있다")
        fun shouldHandleDifferentExceptionTypes() {
            // given
            val exceptions = listOf(
                IllegalStateException("State error"),
                NullPointerException("Null error"),
                IndexOutOfBoundsException("Index error")
            )

            // when & then
            exceptions.forEach { exception ->
                val event = FailedKafkaEvent.create(
                    topic = "test.topic",
                    partitionNumber = 0,
                    offsetNumber = 100L,
                    recordKey = "key",
                    recordValue = "value",
                    consumerGroup = "test-group",
                    exception = exception,
                    retryCount = 1
                )

                assertThat(event.exceptionClass).isEqualTo(exception.javaClass.name)
                assertThat(event.exceptionMessage).isEqualTo(exception.message)
            }
        }
    }

    @Nested
    @DisplayName("markReprocessed 메서드는")
    inner class MarkReprocessedTest {

        @Test
        @DisplayName("이벤트를 재처리 완료 상태로 변경한다")
        fun shouldMarkAsReprocessed() {
            // given
            val event = FailedKafkaEvent.create(
                topic = "test.topic",
                partitionNumber = 0,
                offsetNumber = 100L,
                recordKey = "key",
                recordValue = "value",
                consumerGroup = "test-group",
                exception = RuntimeException("Error"),
                retryCount = 3
            )

            val beforeReprocess = LocalDateTime.now()

            // when
            event.markReprocessed()

            // then
            assertThat(event.status).isEqualTo(FailedEventStatus.REPROCESSED)
            assertThat(event.reprocessedAt).isNotNull()
            assertThat(event.reprocessedAt).isAfterOrEqualTo(beforeReprocess)
        }

        @Test
        @DisplayName("여러 번 호출해도 마지막 시간으로 업데이트된다")
        fun shouldUpdateReprocessedTimeOnMultipleCalls() {
            // given
            val event = FailedKafkaEvent.create(
                topic = "test.topic",
                partitionNumber = 0,
                offsetNumber = 100L,
                recordKey = "key",
                recordValue = "value",
                consumerGroup = "test-group",
                exception = RuntimeException("Error"),
                retryCount = 3
            )

            event.markReprocessed()
            val firstReprocessedAt = event.reprocessedAt

            Thread.sleep(10) // 시간 차이를 위한 대기

            // when
            event.markReprocessed()

            // then
            assertThat(event.reprocessedAt).isAfter(firstReprocessedAt)
        }
    }

    @Nested
    @DisplayName("markIgnored 메서드는")
    inner class MarkIgnoredTest {

        @Test
        @DisplayName("이벤트를 무시 상태로 변경한다")
        fun shouldMarkAsIgnored() {
            // given
            val event = FailedKafkaEvent.create(
                topic = "test.topic",
                partitionNumber = 0,
                offsetNumber = 100L,
                recordKey = "key",
                recordValue = "value",
                consumerGroup = "test-group",
                exception = RuntimeException("Error"),
                retryCount = 3
            )

            // when
            event.markIgnored()

            // then
            assertThat(event.status).isEqualTo(FailedEventStatus.IGNORED)
            assertThat(event.reprocessedAt).isNull()
        }

        @Test
        @DisplayName("여러 번 호출해도 상태가 유지된다")
        fun shouldRemainIgnoredOnMultipleCalls() {
            // given
            val event = FailedKafkaEvent.create(
                topic = "test.topic",
                partitionNumber = 0,
                offsetNumber = 100L,
                recordKey = "key",
                recordValue = "value",
                consumerGroup = "test-group",
                exception = RuntimeException("Error"),
                retryCount = 3
            )

            // when
            event.markIgnored()
            event.markIgnored()

            // then
            assertThat(event.status).isEqualTo(FailedEventStatus.IGNORED)
        }
    }

    @Nested
    @DisplayName("상태 전이 테스트")
    inner class StatusTransitionTest {

        @Test
        @DisplayName("FAILED -> REPROCESSED -> IGNORED 순서로 상태 변경이 가능하다")
        fun shouldTransitionFromFailedToReprocessedToIgnored() {
            // given
            val event = FailedKafkaEvent.create(
                topic = "test.topic",
                partitionNumber = 0,
                offsetNumber = 100L,
                recordKey = "key",
                recordValue = "value",
                consumerGroup = "test-group",
                exception = RuntimeException("Error"),
                retryCount = 3
            )

            // when & then
            assertThat(event.status).isEqualTo(FailedEventStatus.FAILED)

            event.markReprocessed()
            assertThat(event.status).isEqualTo(FailedEventStatus.REPROCESSED)

            event.markIgnored()
            assertThat(event.status).isEqualTo(FailedEventStatus.IGNORED)
        }

        @Test
        @DisplayName("FAILED -> IGNORED 직접 전환이 가능하다")
        fun shouldTransitionDirectlyFromFailedToIgnored() {
            // given
            val event = FailedKafkaEvent.create(
                topic = "test.topic",
                partitionNumber = 0,
                offsetNumber = 100L,
                recordKey = "key",
                recordValue = "value",
                consumerGroup = "test-group",
                exception = RuntimeException("Error"),
                retryCount = 3
            )

            // when
            event.markIgnored()

            // then
            assertThat(event.status).isEqualTo(FailedEventStatus.IGNORED)
            assertThat(event.reprocessedAt).isNull()
        }
    }
}