package com.hirelog.api.auth.infra.jwt

import com.hirelog.api.common.config.security.AuthenticatedMember
import com.hirelog.api.member.domain.MemberRole
import io.jsonwebtoken.Claims
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtUtils: JwtUtils
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        // 1. Access Token 추출
        val token = extractAccessToken(request)

        if (token != null) {
            // 2. 토큰 검증 및 Claims 파싱
            val claims = jwtUtils.parseClaimsSafely(token)

            if (claims != null) {
                // 3. 인증 객체 생성
                val authentication = createAuthentication(claims)

                // 4. SecurityContext 등록
                SecurityContextHolder.getContext().authentication = authentication
            }
        }

        filterChain.doFilter(request, response)
    }

    /**
     * Access Token 쿠키 추출
     */
    private fun extractAccessToken(request: HttpServletRequest): String? =
        request.cookies
            ?.firstOrNull { it.name == "access_token" }
            ?.value

    /**
     * Claims -> Authentication 변환
     */
    private fun createAuthentication(claims: Claims): UsernamePasswordAuthenticationToken {
        val memberId = claims.subject.toLong()
        val roleString = claims["role"] as String

        val authority = SimpleGrantedAuthority("ROLE_$roleString")

        val principal = AuthenticatedMember(
            memberId = memberId,
            role = MemberRole.valueOf(roleString)
        )

        return UsernamePasswordAuthenticationToken(
            principal,
            null,
            listOf(authority)
        )
    }

    /**
     * JWT 필터 제외 경로
     *
     * 책임:
     * - OAuth2 로그인 흐름 방해 금지
     * - 인증/가입 API 방해 금지
     * - 정적 리소스 방해 금지
     */
    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val path = request.requestURI

        return path.startsWith("/oauth2/") ||
                path.startsWith("/login/oauth2/") ||
                path.startsWith("/api/auth/") ||
                path.startsWith("/api/public/") ||
                path.startsWith("/api/health") ||
                path.startsWith("/actuator")
    }

    /**
     * ASYNC 재디스패치는 SecurityConfig에서 permitAll 처리하므로 JWT 필터 스킵.
     * (ASYNC dispatch는 다른 스레드 → ThreadLocal SecurityContext가 비어있어
     *  OncePerRequestFilter의 already-filtered attribute로 재실행이 막혀 있어도
     *  SecurityContext가 채워지지 않아 인가 실패가 발생하는 것을 방지)
     */
    override fun shouldNotFilterAsyncDispatch(): Boolean = true
}
