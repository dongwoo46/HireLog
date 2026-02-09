package com.hirelog.api.auth.infra.jwt

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("JwtUtils 테스트")
class JwtUtilsTest {

    private lateinit var jwtProperties: JwtProperties
    private lateinit var jwtUtils: JwtUtils

    @BeforeEach
    fun setUp() {
        // 테스트용 설정 (Secret 키는 256bit 이상이어야 함)
        jwtProperties = JwtProperties(
            secret = "this-is-a-very-secure-secret-key-for-testing-purpose-only-32bytes",
            accessExpirationMs = 3600000 // 1 hour
        )
        jwtUtils = JwtUtils(jwtProperties)
    }

    @Nested
    @DisplayName("create / validate 테스트")
    inner class TokenLifecycleTest {

        @Test
        @DisplayName("토큰 생성 및 검증이 정상적으로 동작해야 한다")
        fun generate_and_validate() {
            // given
            val memberId = 1L
            val role = "ROLE_USER"

            // when
            val token = jwtUtils.issueAccessToken(memberId, role)

            // then
            assertNotNull(token)
            assertTrue(token.isNotBlank())
            assertTrue(jwtUtils.validate(token), "유효한 토큰이어야 한다")
        }

        @Test
        @DisplayName("토큰에서 정보(Claim)를 올바르게 추출해야 한다")
        fun extract_claims() {
            // given
            val memberId = 123L
            val role = "ROLE_ADMIN"
            val token = jwtUtils.issueAccessToken(memberId, role)

            // when
            val extractedId = jwtUtils.getMemberId(token)
            val extractedRole = jwtUtils.getRole(token)

            // then
            assertEquals(memberId, extractedId)
            assertEquals(role, extractedRole)
        }
    }
}
