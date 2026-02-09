package com.hirelog.api.testinfra

import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

/**
 * PostgreSQL 기반 JPA Repository 테스트 공통 베이스
 *
 * 책임:
 * - 실제 PostgreSQL 환경 제공
 * - JSONB / enum / index 등 운영 스키마 그대로 테스트
 *
 * 비책임:
 * - 비즈니스 로직 테스트 ❌
 * - 서비스 계층 테스트 ❌
 */
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
abstract class PostgresJpaTestBase {

    companion object {

        @Container
        @JvmStatic
        val postgres: PostgreSQLContainer<*> =
            PostgreSQLContainer("postgres:16")
                .withDatabaseName("test")
                .withUsername("test")
                .withPassword("test")

        @JvmStatic
        @DynamicPropertySource
        fun registerDataSource(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)

            // 테스트 전용 DDL 정책
            registry.add("spring.jpa.hibernate.ddl-auto") { "create-drop" }

            // 로그 줄이기 (선택)
            registry.add("spring.jpa.show-sql") { "false" }
        }
    }
}
