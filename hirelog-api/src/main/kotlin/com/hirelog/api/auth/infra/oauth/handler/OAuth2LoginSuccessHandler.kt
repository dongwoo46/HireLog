package com.hirelog.api.auth.infra.oauth.handler

import com.hirelog.api.auth.domain.OAuth2LoginResult
import com.hirelog.api.auth.infra.oauth.user.CustomOAuth2User
import com.hirelog.api.common.config.properties.OAuthFrontendProperties
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.stereotype.Component

/**
 * OAuth2 인증 성공 후 최종 처리를 담당하는 핸들러
 *
 * 주요 역할:
 * 1. 인증된 소셜 정보를 우리 서비스의 회원 상태와 대조 (LoginProcessor)
 * 2. 상태에 맞는 토큰 발급 (정식 서비스용 vs 회원가입 임시용)
 * 3. 프론트엔드의 적절한 페이지로 사용자를 이동(Redirect)
 */
/**
 * OAuth2 로그인 성공 핸들러
 *
 * 책임:
 * - OAuth 인증이 성공한 이후의 후처리 전담
 * - 인증된 OAuthUser를 기준으로 기존 회원 / 신규 회원 분기
 * - 토큰 또는 가입용 임시키 발급
 * - 프론트엔드로 리다이렉트
 *
 * 주의:
 * - 회원 생성/조회 로직 ❌
 * - 토큰 구조/저장 방식 ❌
 *   → 모든 정책 판단은 OAuth2LoginProcessor / OAuth2TokenIssuer에 위임
 */
@Component
class OAuth2LoginSuccessHandler(
    private val loginProcessor: OAuth2LoginProcessor,
    private val tokenIssuer: OAuth2TokenIssuer,
    private val frontendProperties: OAuthFrontendProperties, // 주입받은 설정값
) : AuthenticationSuccessHandler {

    companion object {
        /** 기존 회원 로그인 완료 후 이동 */
        private const val FRONTEND_MAIN = "http://localhost:5173/"

        /** 신규 회원 추가 정보 입력 페이지 */
        private const val FRONTEND_SIGNUP = "http://localhost:5173/signup"
    }

    /**
     * OAuth 인증 성공 시 Spring Security가 호출
     */
    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication,
    ) {
        // OAuth Provider에서 인증된 사용자 정보
        val principal = authentication.principal as CustomOAuth2User
        val oauthUser = principal.oAuthUser

        // OAuthUser 기준으로 로그인 처리 (기존 / 신규 판별)
        when (val result = loginProcessor.loginProcess(oauthUser)) {

            // 기존 회원 → Access/Refresh Token 발급 후 메인 이동
            is OAuth2LoginResult.ExistingUser -> {
                tokenIssuer.issueAccessAndRefresh(
                    memberId = result.memberId,
                    role = result.role,
                    response = response
                )
                response.sendRedirect(frontendProperties.mainUrl)
            }

            // 신규 회원 → 가입용 임시키 발급 후 추가 정보 입력 페이지 이동
            is OAuth2LoginResult.NewUser -> {
                tokenIssuer.issueSignupToken(
                    oauthUser = oauthUser,
                    response = response
                )
                response.sendRedirect(frontendProperties.signupUrl)
            }
        }
    }
}
