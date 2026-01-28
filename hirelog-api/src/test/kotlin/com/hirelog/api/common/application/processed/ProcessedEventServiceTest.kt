package com.hirelog.api.common.application.processed

import com.hirelog.api.common.domain.ProcessedEventId
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.Collections
import java.util.UUID

/**
 * ProcessedEventServiceTest
 *
 * 목적:
 * - Kafka 멱등성 처리 로직의 실제 동작 검증
 *
 * 범위:
 * - ApplicationService
 * - JPA Adapter / Repository
 * - 실제 PostgreSQL (Testcontainers)
 */
@SpringBootTest
@Testcontainers
class ProcessedEventServiceTest {

    companion object {

        @Container
        private val postgres = PostgreSQLContainer("postgres:16").apply {
            withDatabaseName("testdb")
            withUsername("test")
            withPassword("test")
        }

        @JvmStatic
        @DynamicPropertySource
        fun registerProps(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
        }
    }

    @Autowired
    private lateinit var processedEventService: ProcessedEventService

    /**
     * 테스트 1
     *
     * 최초 이벤트는
     * - 아직 처리되지 않은 이벤트로 판단되어야 한다
     * - false를 반환해야 한다
     */
    @Test
    fun `최초 이벤트는 처리되지 않은 이벤트로 판단된다`() {
        val eventId = ProcessedEventId.from(UUID.randomUUID().toString())
        val consumerGroup = "test-consumer-group"

        val result = processedEventService.isAlreadyProcessedOrMark(
            eventId = eventId,
            consumerGroup = consumerGroup
        )

        assertFalse(result)
    }

    /**
     * 테스트 2
     *
     * 이미 처리된 이벤트는
     * - 다시 처리되지 않아야 한다
     * - true를 반환해야 한다
     */
    @Test
    fun `이미 처리된 이벤트는 다시 처리되지 않는다`() {
        val eventId = ProcessedEventId.from(UUID.randomUUID().toString())
        val consumerGroup = "test-consumer-group"

        // 최초 처리
        processedEventService.isAlreadyProcessedOrMark(
            eventId = eventId,
            consumerGroup = consumerGroup
        )

        // 동일 이벤트 재처리
        val result = processedEventService.isAlreadyProcessedOrMark(
            eventId = eventId,
            consumerGroup = consumerGroup
        )

        assertTrue(result)
    }

    /**
     * 테스트 3 (핵심)
     *
     * 동일한 이벤트가 동시에 처리되더라도
     * - 하나만 최초 처리(false)
     * - 나머지는 중복 처리(true)
     *
     * DB 복합 PK 제약을 통해
     * 멱등성이 최종 보장되어야 한다
     */
    @Test
    fun `동시에 동일 이벤트를 처리해도 하나만 최초 처리된다`() {
        val eventId = ProcessedEventId.from(UUID.randomUUID().toString())
        val consumerGroup = "test-consumer-group"

        val results = Collections.synchronizedList(mutableListOf<Boolean>())

        val threads = List(2) {
            Thread {
                try {
                    val result = processedEventService.isAlreadyProcessedOrMark(
                        eventId = eventId,
                        consumerGroup = consumerGroup
                    )
                    results.add(result)
                } catch (e: Exception) {
                    // unique constraint 충돌 등 예외 발생 시
                    // 이미 처리된 이벤트로 간주
                    results.add(true)
                }
            }
        }

        threads.forEach { it.start() }
        threads.forEach { it.join() }

        assertEquals(2, results.size)
        assertEquals(1, results.count { it })     // 중복 처리
        assertEquals(1, results.count { !it })    // 최초 처리
    }
}
