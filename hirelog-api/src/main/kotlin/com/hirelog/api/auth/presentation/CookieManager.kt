package com.hirelog.api.auth.presentation

import com.hirelog.api.auth.application.dto.AuthTokens
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component

@Component
class CookieManager {

    fun setAuthCookies(response: HttpServletResponse, tokens: AuthTokens) {
        addCookie(response, "access_token", tokens.accessToken, 3600) // 1시간
        addCookie(response, "refresh_token", tokens.refreshToken, 604800) // 7일
    }

    fun setSignupCookie(response: HttpServletResponse, signupToken: String) {
        addCookie(response, "signup_token", signupToken, 600) // 10분
    }

    fun clearAuthCookies(response: HttpServletResponse) {
        addCookie(response, "access_token", "", 0)
        addCookie(response, "refresh_token", "", 0)
    }

    private fun addCookie(response: HttpServletResponse, name: String, value: String, maxAge: Int) {
        val cookie = Cookie(name, value).apply {
            path = "/"
            isHttpOnly = true
            secure = false // HTTPS 환경에서 활성화
            this.maxAge = maxAge
        }
        response.addCookie(cookie)
    }
}