package com.hirelog.api.common.config

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@Configuration
class JacksonConfig {

    @Bean
    @Primary // 프로젝트 기본 ObjectMapper로 설정
    fun objectMapper(): ObjectMapper {
        return ObjectMapper()
            .registerKotlinModule() // Kotlin data class 지원
            .registerModule(JavaTimeModule()) // LocalDateTime, Instant 지원
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS) // 날짜를 ISO-8601 문자열로 저장
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false) // 없는 필드는 무시 (유연함)
    }
}