package com.hirelog.api.common.application.kafka

import com.hirelog.api.common.domain.kafka.FailedKafkaEvent
import com.hirelog.api.common.infra.persistence.jpa.repository.FailedKafkaEventJpaRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.lang.RuntimeException

@DisplayName("FailedKafkaEventService 테스트")
class FailedKafkaEventServiceTest {

    private val repository: FailedKafkaEventJpaRepository = mockk()
    private val service = FailedKafkaEventService(repository)

    @Nested
    @DisplayName("save 테스트")
    inner class SaveTest {

        @Test
        @DisplayName("성공: 실패 이벤트를 저장해야 한다")
        fun success() {
            // given
            every { repository.save(any()) } answers { firstArg() }

            // when
            service.save(
                topic = "topic",
                partition = 1,
                offset = 100L,
                key = "key",
                value = "val",
                consumerGroup = "group",
                exception = RuntimeException("Error"),
                retryCount = 3
            )

            // then
            verify(exactly = 1) { repository.save(any()) }
        }

        @Test
        @DisplayName("실패: 저장 중 예외가 발생해도 삼켜야 한다 (DLT 전송 등 후속 처리를 위해)")
        fun fail_suppress_exception() {
            // given
            every { repository.save(any()) } throws RuntimeException("DB Save Error")

            // when
            // 예외가 발생하지 않아야 함
            service.save(
                topic = "topic",
                partition = 1,
                offset = 100L,
                key = "key",
                value = "val",
                consumerGroup = "group",
                exception = RuntimeException("Error"),
                retryCount = 3
            )

            // then
            verify(exactly = 1) { repository.save(any()) }
        }
    }
}
