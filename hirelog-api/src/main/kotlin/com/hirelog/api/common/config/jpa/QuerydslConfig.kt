package com.hirelog.api.common.config

import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Querydsl 설정
 *
 * 책임:
 * - JPAQueryFactory Bean 등록
 * - 모든 Querydsl Repository에서 공용으로 사용
 */
@Configuration
class QuerydslConfig(
    private val entityManager: EntityManager
) {

    @Bean
    fun jpaQueryFactory(): JPAQueryFactory {
        // EntityManager 기반 Querydsl 팩토리 생성
        return JPAQueryFactory(entityManager)
    }
}
