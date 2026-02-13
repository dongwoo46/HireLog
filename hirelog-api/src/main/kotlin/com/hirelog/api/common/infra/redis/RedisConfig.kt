package com.hirelog.api.common.infra.redis

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
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
class RedisConfig(
    private val redisProperties: RedisProperties
) {

    /**
     * Redis 연결 팩토리
     *
     * 수정사항:
     * - RedisStandaloneConfiguration을 사용하여 비밀번호 설정
     */
    @Bean
    fun redisConnectionFactory(): RedisConnectionFactory {
        val config = RedisStandaloneConfiguration().apply {
            hostName = redisProperties.host
            port = redisProperties.port
            setPassword(redisProperties.password)  // ✅ 비밀번호 설정
        }

        return LettuceConnectionFactory(config)
    }

    /* =========================
     * Redis ObjectMapper (핵심)
     * ========================= */

    /**
     * Redis 전용 ObjectMapper
     * * 보완사항:
     * - JavaTimeModule 추가 (Instant, LocalDateTime 처리용)
     * - DefaultTyping 설정 (JSON에 클래스 정보를 포함하여 역직렬화 오류 방지)
     */
    @Bean
    fun redisObjectMapper(): ObjectMapper =
        ObjectMapper()
            .registerModule(KotlinModule.Builder().build())
            .registerModule(com.fasterxml.jackson.datatype.jsr310.JavaTimeModule()) // 날짜 모듈 추가
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .activateDefaultTyping(
                com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY
            )

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

    /**
     * 객체(Any) 저장을 위한 템플릿 수정
     * * 수정사항:
     * - GenericJackson2JsonRedisSerializer 생성 시 redisObjectMapper()를 주입함
     */
    @Bean
    fun redisTemplate(
        connectionFactory: RedisConnectionFactory,
        redisObjectMapper: ObjectMapper // 위에서 정의한 빈 주입
    ): RedisTemplate<String, Any> {
        val template = RedisTemplate<String, Any>()
        template.setConnectionFactory(connectionFactory)

        // 중요: 커스텀 ObjectMapper가 적용된 Serializer 사용
        val serializer = GenericJackson2JsonRedisSerializer(redisObjectMapper)

        template.keySerializer = StringRedisSerializer()
        template.valueSerializer = serializer
        template.hashKeySerializer = StringRedisSerializer()
        template.hashValueSerializer = serializer

        template.afterPropertiesSet()
        return template
    }
}
