package com.hirelog.api.common.presentation.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

/**
 * HTTP 요청 진입 시 MDC에 requestId 주입
 *
 * 우선순위:
 * 1. X-Request-ID 헤더 값 사용 (클라이언트/게이트웨이가 제공한 경우)
 * 2. 없으면 UUID 신규 생성
 *
 * 정리: try-finally MDC.clear()로 스레드 풀 오염 방지
 */
@Component
class MdcRequestFilter : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val requestId = request.getHeader("X-Request-ID")?.takeIf { it.isNotBlank() }
            ?: UUID.randomUUID().toString()

        MDC.put("requestId", requestId)
        try {
            filterChain.doFilter(request, response)
        } finally {
            MDC.clear()
        }
    }
}
