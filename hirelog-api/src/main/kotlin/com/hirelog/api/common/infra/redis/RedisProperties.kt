package com.hirelog.api.common.infra.redis

import org.springframework.boot.context.properties.ConfigurationProperties
/**
 * RedisProperties
 *
 * 책임:
 * - application.yml의 spring.data.redis 설정 바인딩
 *
 * 비책임:
 * - RedisConnection 생성 ❌
 * - RedisTemplate 구성 ❌
 */
@ConfigurationProperties(prefix = "spring.data.redis")
data class RedisProperties(
    val host: String = "",
    val port: Int = 6379
)