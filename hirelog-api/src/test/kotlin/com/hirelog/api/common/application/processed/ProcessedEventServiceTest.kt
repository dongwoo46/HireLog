package com.hirelog.api.common.application.processed

import com.hirelog.api.common.domain.process.ProcessedEventId
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@SpringBootTest
class ProcessedEventServiceTest {

    companion object {
        // PostgreSQLContainer 대신 GenericContainer를 사용합니다.
        // 이렇게 하면 JUnit 4의 TestRule을 상속받지 않아 컴파일 에러가 발생하지 않습니다.
        private val postgres = GenericContainer("postgres:16")
            .withExposedPorts(5432)
            .withEnv("POSTGRES_DB", "testdb")
            .withEnv("POSTGRES_USER", "test")
            .withEnv("POSTGRES_PASSWORD", "test")
            .apply { start() }

        @JvmStatic
        @DynamicPropertySource
        fun registerProps(registry: DynamicPropertyRegistry) {
            val host = postgres.host
            val port = postgres.getMappedPort(5432)

            // JDBC URL을 직접 생성하여 주입합니다.
            registry.add("spring.datasource.url") { "jdbc:postgresql://$host:$port/testdb" }
            registry.add("spring.datasource.username") { "test" }
            registry.add("spring.datasource.password") { "test" }
        }
    }

    @Autowired
    private lateinit var processedEventService: ProcessedEventService

    @Test
    fun `최초 이벤트는 처리되지 않은 이벤트로 판단된다`() {
        val eventId = ProcessedEventId.create(UUID.randomUUID().toString())
        val consumerGroup = "test-group"
        assertFalse(processedEventService.isAlreadyProcessedOrMark(eventId, consumerGroup))
    }

    @Test
    fun `이미 처리된 이벤트는 다시 처리되지 않는다`() {
        val eventId = ProcessedEventId.create(UUID.randomUUID().toString())
        val consumerGroup = "test-group"
        processedEventService.isAlreadyProcessedOrMark(eventId, consumerGroup)
        assertTrue(processedEventService.isAlreadyProcessedOrMark(eventId, consumerGroup))
    }

    @Test
    fun `동시에 동일 이벤트를 처리해도 하나만 최초 처리된다`() {
        val eventId = ProcessedEventId.create(UUID.randomUUID().toString())
        val consumerGroup = "concurrent-group"
        val threadCount = 10
        val startLatch = CountDownLatch(1)
        val endLatch = CountDownLatch(threadCount)
        val results = Collections.synchronizedList(mutableListOf<Boolean>())

        repeat(threadCount) {
            Thread {
                startLatch.await()
                try {
                    results.add(processedEventService.isAlreadyProcessedOrMark(eventId, consumerGroup))
                } catch (e: Exception) {
                    results.add(true)
                } finally {
                    endLatch.countDown()
                }
            }.start()
        }

        startLatch.countDown()
        endLatch.await(10, TimeUnit.SECONDS)

        assertEquals(1, results.count { !it })
    }
}