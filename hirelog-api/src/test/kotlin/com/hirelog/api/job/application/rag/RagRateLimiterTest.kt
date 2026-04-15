package com.hirelog.api.job.application.rag

import com.hirelog.api.common.exception.BusinessErrorCode
import com.hirelog.api.common.exception.BusinessException
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ValueOperations

@DisplayName("RagRateLimiter 테스트")
class RagRateLimiterTest {

    private lateinit var rateLimiter: RagRateLimiter
    private lateinit var redisTemplate: RedisTemplate<String, Any>
    private lateinit var opsForValue: ValueOperations<String, Any>

    @BeforeEach
    fun setUp() {
        redisTemplate = mockk()
        opsForValue = mockk()
        every { redisTemplate.opsForValue() } returns opsForValue
        rateLimiter = RagRateLimiter(redisTemplate)
    }

    @Nested
    @DisplayName("ADMIN 권한은")
    inner class AdminTest {

        @Test
        @DisplayName("Redis를 조회하지 않고 제한 없이 통과한다")
        fun shouldPassWithoutRedisInteraction() {
            // Act & Assert — 예외 없이 통과
            rateLimiter.checkAndIncrement(memberId = 1L, isAdmin = true)

            verify(exactly = 0) { opsForValue.increment(any()) }
        }
    }

    @Nested
    @DisplayName("USER 권한은")
    inner class UserTest {

        @Test
        @DisplayName("첫 번째 호출(count=1)이면 TTL을 설정하고 통과한다")
        fun shouldSetTtlOnFirstCallAndPass() {
            // Arrange
            every { opsForValue.increment(any<String>()) } returns 1L
            every { redisTemplate.expire(any<String>(), any()) } returns true

            // Act — 예외 없이 통과해야 함
            rateLimiter.checkAndIncrement(memberId = 100L, isAdmin = false)

            // Assert — 첫 호출에만 TTL 설정
            verify(exactly = 1) { redisTemplate.expire(any<String>(), any()) }
        }

        @Test
        @DisplayName("두 번째 이후 호출은 TTL을 재설정하지 않는다")
        fun shouldNotResetTtlOnSubsequentCalls() {
            // Arrange
            every { opsForValue.increment(any<String>()) } returns 2L

            // Act
            rateLimiter.checkAndIncrement(memberId = 100L, isAdmin = false)

            // Assert — TTL 재설정 없음
            verify(exactly = 0) { redisTemplate.expire(any<String>(), any()) }
        }

        @Test
        @DisplayName("하루 한도(3회) 이내이면 예외가 발생하지 않는다")
        fun shouldNotThrowWhenWithinDailyLimit() {
            // Arrange — count=3 (한도 정확히 도달)
            every { opsForValue.increment(any<String>()) } returns 3L

            // Act & Assert
            rateLimiter.checkAndIncrement(memberId = 100L, isAdmin = false)
        }

        @Test
        @DisplayName("하루 한도(3회) 초과 시 BusinessException(RAG_RATE_LIMIT_EXCEEDED)을 던진다")
        fun shouldThrowBusinessExceptionWhenDailyLimitExceeded() {
            // Arrange — count=4 (한도 초과)
            every { opsForValue.increment(any<String>()) } returns 4L

            // Act & Assert
            assertThatThrownBy {
                rateLimiter.checkAndIncrement(memberId = 100L, isAdmin = false)
            }.isInstanceOf(BusinessException::class.java)
                .satisfies({ e ->
                    val ex = e as BusinessException
                    assertThat(ex.errorCode).isEqualTo(BusinessErrorCode.RAG_RATE_LIMIT_EXCEEDED)
                })
        }

        @Test
        @DisplayName("memberId가 다르면 서로 독립적인 카운터를 사용한다")
        fun shouldUseIndependentCounterPerMember() {
            // Arrange — 두 사용자 모두 첫 번째 호출
            every { opsForValue.increment(match { it.contains(":100:") }) } returns 1L
            every { opsForValue.increment(match { it.contains(":200:") }) } returns 1L
            every { redisTemplate.expire(any<String>(), any()) } returns true

            // Act
            rateLimiter.checkAndIncrement(memberId = 100L, isAdmin = false)
            rateLimiter.checkAndIncrement(memberId = 200L, isAdmin = false)

            // Assert — 각각 TTL 설정
            verify(exactly = 2) { redisTemplate.expire(any<String>(), any()) }
        }
    }
}
