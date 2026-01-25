package com.yourpackage.common.infra.redis.messaging

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.serializer.StringRedisSerializer

/**
 * Redis 공통 설정
 *
 * 책임:
 * - Redis 연결 설정
 * - RedisTemplate 구성
 *
 * 비책임:
 * - Stream Key 정의 ❌
 * - 메시징 정책 ❌
 * - 도메인 의미 ❌
 */
@Configuration
class RedisConfig {

    /**
     * Redis 연결 팩토리
     *
     * 주의:
     * - 단일 Redis 인스턴스 기준
     * - Sentinel / Cluster는 추후 확장
     */
    @Bean
    fun redisConnectionFactory(): RedisConnectionFactory {
        return LettuceConnectionFactory()
    }

    /**
     * Redis 전용
     *
     * 특징:
     * - Redis와 주고받는 모든 key/value를 String 기준으로 처리한다
     * - Stream 메시지의 최소 단위 보장
     */
    @Bean
    fun stringRedisTemplate(
        connectionFactory: RedisConnectionFactory
    ): StringRedisTemplate {

        val template = StringRedisTemplate()
        template.setConnectionFactory(connectionFactory)

        // Key / Value 직렬화 명확화
        template.keySerializer = StringRedisSerializer()
        template.valueSerializer = StringRedisSerializer()
        template.hashKeySerializer = StringRedisSerializer()
        template.hashValueSerializer = StringRedisSerializer()

        template.afterPropertiesSet()
        return template
    }
}
