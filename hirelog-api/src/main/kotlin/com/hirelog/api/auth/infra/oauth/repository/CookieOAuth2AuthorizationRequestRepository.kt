package com.hirelog.api.auth.infra.oauth.repository

import com.fasterxml.jackson.databind.ObjectMapper
import com.hirelog.api.auth.infra.oauth.user.OAuth2AuthRequestCookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames
import org.springframework.stereotype.Component
import java.util.Base64

/**
 * Cookie 기반 OAuth2AuthorizationRequestRepository
 *
 * 책임:
 * - OAuth2 AuthorizationRequest를 HttpSession 대신 Cookie에 저장
 *
 * 목적:
 * - OAuth 로그인 흐름을 JWT 기반 Stateless 구조로 유지
 */
@Component
class CookieOAuth2AuthorizationRequestRepository(
    private val objectMapper: ObjectMapper
) : AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    companion object {
        /** OAuth AuthorizationRequest 저장용 Cookie 이름 */
        private const val COOKIE_NAME = "OAUTH2_AUTH_REQUEST"

        /** OAuth 인증 플로우 유효 시간 (초) */
        private const val MAX_AGE_SECONDS = 180

        /** 내부 계약 충족용 더미 Authorization URI */
        private const val DUMMY_AUTHORIZATION_URI = "https://oauth2.internal/dummy"
    }

    /**
     * OAuth Provider 콜백 시 호출
     * - Cookie에 저장된 AuthorizationRequest 복원
     */
    override fun loadAuthorizationRequest(
        request: HttpServletRequest
    ): OAuth2AuthorizationRequest? {
        val cookie = request.cookies
            ?.firstOrNull { it.name == COOKIE_NAME }
            ?: return null

        return try {
            val decoded = String(Base64.getUrlDecoder().decode(cookie.value))
            val saved = objectMapper.readValue(decoded, OAuth2AuthRequestCookie::class.java)

            // ⭐ registrationId를 attributes에 저장하여 Spring Security가 사용할 수 있게 함
            OAuth2AuthorizationRequest.authorizationCode()
                .state(saved.state)
                .redirectUri(saved.redirectUri)
                .authorizationUri(DUMMY_AUTHORIZATION_URI)
                .clientId("dummy")
                .attributes { attrs ->
                    attrs[OAuth2ParameterNames.REGISTRATION_ID] = saved.registrationId
                }
                .build()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * OAuth 인증 시작 시 호출
     * - AuthorizationRequest를 Cookie에 저장
     */
    override fun saveAuthorizationRequest(
        authorizationRequest: OAuth2AuthorizationRequest?,
        request: HttpServletRequest,
        response: HttpServletResponse
    ) {
        if (authorizationRequest == null) {
            removeAuthorizationRequest(request, response)
            return
        }

        // OAuth 인증 시작 URL: /oauth2/authorization/{registrationId}
        val registrationId = request.requestURI.substringAfterLast("/")

        val payload = OAuth2AuthRequestCookie(
            registrationId = registrationId,
            state = authorizationRequest.state,
            redirectUri = authorizationRequest.redirectUri
        )

        val encoded = Base64.getUrlEncoder()
            .encodeToString(objectMapper.writeValueAsBytes(payload))

        val secureTag = if (request.isSecure) "; Secure" else ""

        response.addHeader(
            "Set-Cookie",
            "$COOKIE_NAME=$encoded; Path=/; Max-Age=$MAX_AGE_SECONDS; HttpOnly; SameSite=Lax$secureTag"
        )
    }

    /**
     * OAuth 성공/실패 후 호출
     * - AuthorizationRequest Cookie 제거
     */
    override fun removeAuthorizationRequest(
        request: HttpServletRequest,
        response: HttpServletResponse
    ): OAuth2AuthorizationRequest? {
        // 이후 인증 단계에서 사용될 수 있으므로 마지막 요청 반환
        val lastRequest = loadAuthorizationRequest(request)

        response.addHeader(
            "Set-Cookie",
            "$COOKIE_NAME=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax"
        )

        return lastRequest
    }
}
