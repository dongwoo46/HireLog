package com.hirelog.api.common.infra.redis

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class RedisService(
    private val redisTemplate: RedisTemplate<String, Any>,
    private val redisObjectMapper: ObjectMapper // RedisConfig에서 정의한 빈이 주입됩니다.
) {
    /**
     * 값 저장
     */
    fun set(key: String, value: Any, duration: Duration) {
        redisTemplate.opsForValue().set(key, value, duration)
    }

    /**
     * 타입 안전 조회 (수정됨)
     * * 단순 캐스팅 대신 ObjectMapper를 사용하여
     * LinkedHashMap 등으로 역직렬화된 데이터를 실제 DTO 객체로 변환합니다.
     */
    fun <T : Any> get(key: String, type: Class<T>): T? {
        val value = redisTemplate.opsForValue().get(key) ?: return null

        return try {
            // value가 이미 원하는 타입이면 그대로 반환,
            // Map 형태라면 DTO로 변환(re-mapping)합니다.
            redisObjectMapper.convertValue(value, type)
        } catch (e: Exception) {
            // 로그를 남겨두면 나중에 디버깅하기 훨씬 편합니다.
            // logger.error("Redis 역직렬화 에러: ${e.message}")
            null
        }
    }

    /**
     * 데이터 삭제
     */
    fun delete(key: String) {
        redisTemplate.delete(key)
    }

    /**
     * 키 존재 여부 확인
     */
    fun hasKey(key: String): Boolean {
        // null safe 처리를 위해 == true 추가
        return redisTemplate.hasKey(key) == true
    }
}