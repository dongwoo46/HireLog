package com.hirelog.api.member.presentation

import com.hirelog.api.common.config.properties.OAuthFrontendProperties
import com.hirelog.api.member.application.SignupFacadeService
import com.hirelog.api.member.presentation.dto.BindRequest
import com.hirelog.api.member.presentation.dto.CheckEmailRequest
import com.hirelog.api.member.presentation.dto.CheckEmailResponse
import com.hirelog.api.member.presentation.dto.SignupCompleteRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * 회원가입 및 계정 연동 컨트롤러
 *
 * 책임:
 * - 클라이언트 요청 바인딩 및 유효성 검증 (@Valid)
 * - 가입 세션 토큰(signup_token) 전달
 * - 비즈니스 결과에 따른 HTTP 응답(Redirect, JSON) 제어
 *
 * 설계 포인트:
 * - Thin Controller: 실제 로직은 SignupFacadeService로 전적으로 위임
 * - 용어 통일: 모든 노출 필드명은 'username'으로 고정
 */
@RestController
@RequestMapping("/api/auth/signup")
class SignupController(
    private val signupFacadeService: SignupFacadeService,
    private val frontendProperties: OAuthFrontendProperties,
) {

    /**
     * 이메일 중복 체크 및 가입 가능 여부 확인
     *
     * - signup_token이 유효한 세션인지 확인 후, 이메일 존재 여부 반환
     */
    @PostMapping("/check-email")
    fun checkEmail(
        @Valid @RequestBody request: CheckEmailRequest,
        @CookieValue("signup_token") signupToken: String,
    ): ResponseEntity<CheckEmailResponse> {
        val response = signupFacadeService.checkEmailAvailability(signupToken, request.email)
        return ResponseEntity.ok(response)
    }

    /**
     * 기존 회원 계정 연동 (Binding)
     *
     * - 중복 체크 결과 이미 존재하는 계정일 경우 호출
     * - 연동 성공 시 JWT 발급 후 메인 페이지로 리다이렉트
     */
    @PostMapping("/bind")
    fun bind(
        @Valid @RequestBody request: BindRequest,
        @CookieValue("signup_token") signupToken: String,
        response: HttpServletResponse,
    ) {
        signupFacadeService.bindOAuthAccount(signupToken, request, response)

        // 가입 완료 후 클라이언트 앱 진입점으로 이동
        response.sendRedirect(frontendProperties.mainUrl)
    }

    /**
     * 신규 회원 가입 완료 (Provisioning)
     *
     * - 신규 사용자 정보 입력 후 최종 가입 시 호출
     * - 계정 생성 및 연동 후 JWT 발급, 메인 페이지로 리다이렉트
     */
    @PostMapping("/complete")
    fun complete(
        @Valid @RequestBody request: SignupCompleteRequest,
        @CookieValue("signup_token") signupToken: String,
        response: HttpServletResponse,
    ): ResponseEntity<Long> { // Redirect 대신 성공 ID 반환
        val memberId = signupFacadeService.completeSignup(signupToken, request, response)
        return ResponseEntity.ok(memberId)
    }
}