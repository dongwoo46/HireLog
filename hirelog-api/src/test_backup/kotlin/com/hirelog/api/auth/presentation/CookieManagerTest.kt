package com.hirelog.api.auth.presentation

import com.hirelog.api.auth.application.dto.AuthTokens
import io.mockk.every
import io.mockk.mockk
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("CookieManager 테스트")
class CookieManagerTest {

    private val cookieManager = CookieManager()

    @Test
    @DisplayName("setAuthCookies: Access/Refresh 토큰 쿠키가 올바른 속성으로 설정된다")
    fun set_auth_cookies() {
        // given
        val response = mockk<HttpServletResponse>(relaxed = true)
        val tokens = AuthTokens(
            accessToken = "access-123",
            refreshToken = "refresh-456"
        )

        val capturedCookies = mutableListOf<Cookie>()
        every { response.addCookie(capture(capturedCookies)) } returns Unit

        // when
        cookieManager.setAuthCookies(response, tokens)

        // then
        assertEquals(2, capturedCookies.size)

        val accessCookie = capturedCookies.first { it.name == "access_token" }
        assertEquals("access-123", accessCookie.value)
        assertTrue(accessCookie.isHttpOnly)
        assertTrue(accessCookie.secure)
        assertEquals("/", accessCookie.path)
        assertEquals(3600, accessCookie.maxAge)

        val refreshCookie = capturedCookies.first { it.name == "refresh_token" }
        assertEquals("refresh-456", refreshCookie.value)
        assertTrue(refreshCookie.isHttpOnly)
        assertTrue(refreshCookie.secure)
        assertEquals("/", refreshCookie.path)
        assertEquals(604800, refreshCookie.maxAge)
    }

    @Test
    @DisplayName("clearAuthCookies: Access/Refresh 토큰 쿠키가 만료된다")
    fun clear_auth_cookies() {
        // given
        val response = mockk<HttpServletResponse>(relaxed = true)
        val capturedCookies = mutableListOf<Cookie>()
        every { response.addCookie(capture(capturedCookies)) } returns Unit

        // when
        cookieManager.clearAuthCookies(response)

        // then
        assertEquals(2, capturedCookies.size)

        capturedCookies.forEach { cookie ->
            assertEquals(0, cookie.maxAge)
            assertEquals("/", cookie.path)
        }
    }
}
