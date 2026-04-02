package com.hirelog.api.auth.presentation

import com.hirelog.api.auth.application.PasswordLoginService
import com.hirelog.api.auth.application.TokenRefreshService
import com.hirelog.api.auth.application.dto.AuthTokens
import com.hirelog.api.auth.presentation.dto.PasswordLoginReq
import com.hirelog.api.auth.presentation.dto.TokenRefreshRes
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Auth Controller
 *
 * 책임:
 * - Password Login
 * - Token Refresh
 * - Logout
 */
@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val tokenRefreshService: TokenRefreshService,
    private val passwordLoginService: PasswordLoginService,
    private val cookieManager: CookieManager
) {

    /**
     * 이메일/비밀번호 로그인
     */
    @PostMapping("/login")
    fun login(
        @Valid @RequestBody request: PasswordLoginReq,
        response: HttpServletResponse
    ): ResponseEntity<TokenRefreshRes> {
        val tokens = passwordLoginService.login(
            email = request.email,
            password = request.password
        )

        cookieManager.setAuthCookies(
            response = response,
            tokens = AuthTokens(
                accessToken = tokens.accessToken,
                refreshToken = tokens.refreshToken
            )
        )

        return ResponseEntity.ok(
            TokenRefreshRes(
                accessToken = tokens.accessToken,
                refreshToken = tokens.refreshToken
            )
        )
    }

    /**
     * Access Token 재발급
     *
     * 입력:
     * - Cookie에서 refresh_token 읽기
     *
     * 응답:
     * - 성공: 새 Access Token + Refresh Token (Cookie + Body)
     * - 실패: 401 Unauthorized
     */
    @PostMapping("/refresh")
    fun refresh(
        @CookieValue(name = "refresh_token", required = false) refreshToken: String?,
        response: HttpServletResponse
    ): ResponseEntity<TokenRefreshRes> {

        if (refreshToken == null) {
            return ResponseEntity.status(401).build()
        }

        val result = tokenRefreshService.refresh(refreshToken)
            ?: return ResponseEntity.status(401).build()

        // Cookie 설정
        addCookie(response, "access_token", result.accessToken, 3600)       // 1시간
        addCookie(response, "refresh_token", result.refreshToken, 604800)  // 7일

        return ResponseEntity.ok(
            TokenRefreshRes(
                accessToken = result.accessToken,
                refreshToken = result.refreshToken
            )
        )
    }

    /**
     * 로그아웃
     *
     * 처리:
     * - Refresh Token 무효화(Redis 삭제)
     * - Cookie 삭제
     */
    @PostMapping("/logout")
    fun logout(
        @CookieValue(name = "refresh_token", required = false) refreshToken: String?,
        response: HttpServletResponse
    ): ResponseEntity<Void> {

        // Refresh Token 무효화
        refreshToken?.let {
            tokenRefreshService.invalidate(it)
        }

        // Cookie 삭제
        deleteCookie(response, "access_token")
        deleteCookie(response, "refresh_token")

        return ResponseEntity.ok().build()
    }

    private fun addCookie(response: HttpServletResponse, name: String, value: String, maxAge: Int) {
        val cookie = Cookie(name, value).apply {
            path = "/"
            isHttpOnly = true
            // secure = true // HTTPS 환경에서 활성화
            this.maxAge = maxAge
        }
        response.addCookie(cookie)
    }

    private fun deleteCookie(response: HttpServletResponse, name: String) {
        val cookie = Cookie(name, "").apply {
            path = "/"
            isHttpOnly = true
            maxAge = 0
        }
        response.addCookie(cookie)
    }
}

