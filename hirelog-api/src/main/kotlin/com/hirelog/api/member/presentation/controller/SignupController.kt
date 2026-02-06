package com.hirelog.api.member.presentation.controller

import com.hirelog.api.auth.application.dto.AuthTokens
import com.hirelog.api.auth.presentation.CookieManager
import com.hirelog.api.member.application.SignupFacadeService
import com.hirelog.api.member.presentation.dto.*
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
 * - 비즈니스 결과에 따른 HTTP 응답(쿠키, JSON) 제어
 *
 * 설계 포인트:
 * - Thin Controller: 실제 로직은 SignupFacadeService로 전적으로 위임
 * - ✅ 쿠키 설정은 CookieManager 사용 (Presentation 계층 책임)
 */
@RestController
@RequestMapping("/api/auth/signup")
class SignupController(
    private val signupFacadeService: SignupFacadeService,
    private val cookieManager: CookieManager,  // ✅ CookieManager 주입
) {

    /**
     * 이메일 중복 체크 및 인증코드 발송
     *
     * - 중복 아님: 인증코드 발송 후 exists=false 반환
     * - 중복: exists=true 반환 (프론트에서 연결 여부 선택)
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
     * 인증코드 발송 (기존 계정 연결 선택 시)
     */
    @PostMapping("/send-code")
    fun sendCode(
        @Valid @RequestBody request: SendCodeRequest,
        @CookieValue("signup_token") signupToken: String,
    ): ResponseEntity<Void> {
        signupFacadeService.sendVerificationCode(signupToken, request.email)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/recovery/send-code")
    fun sendRecoveryCode(
        @Valid @RequestBody request: SendCodeRequest,
        @CookieValue("recovery_token") recoveryToken: String,
    ): ResponseEntity<Void> {
        signupFacadeService.sendRecoveryVerificationCode(recoveryToken, request.email)
        return ResponseEntity.ok().build()
    }

    /**
     * 인증코드 검증
     */
    @PostMapping("/verify-code")
    fun verifyCode(
        @Valid @RequestBody request: VerifyCodeRequest,
        @CookieValue("signup_token") signupToken: String,
    ): ResponseEntity<VerifyCodeResponse> {
        val response = signupFacadeService.verifyCode(signupToken, request.email, request.code)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/recovery/verify-code")
    fun verifyRecoveryCode(
        @Valid @RequestBody request: VerifyCodeRequest,
        @CookieValue("recovery_token") recoveryToken: String,
    ): ResponseEntity<VerifyCodeResponse> {
        val response = signupFacadeService.verifyRecoveryCode(
            recoveryToken = recoveryToken,
            email = request.email,
            code = request.code
        )
        return ResponseEntity.ok(response)
    }


    /**
     * 기존 회원 계정 연동 (Binding)
     *
     * - 중복 체크 결과 이미 존재하는 계정일 경우 호출
     * - 연동 성공 시 JWT 쿠키 발급
     *
     * ✅ Service에서 받은 토큰으로 쿠키 설정
     */
    @PostMapping("/bind")
    fun bind(
        @Valid @RequestBody request: BindRequest,
        @CookieValue("signup_token") signupToken: String,
        response: HttpServletResponse,
    ): ResponseEntity<MemberIdResponse> {
        // Service 호출 (토큰 받기)
        val result = signupFacadeService.bindOAuthAccount(signupToken, request)

        // ✅ Presentation 계층에서 쿠키 설정
        cookieManager.setAuthCookies(
            response = response,
            tokens = AuthTokens(
                accessToken = result.accessToken,
                refreshToken = result.refreshToken
            )
        )

        return ResponseEntity.ok(MemberIdResponse(memberId = result.memberId))
    }

    /**
     * 신규 회원 가입 완료 (Provisioning)
     *
     * - 신규 사용자 정보 입력 후 최종 가입 시 호출
     * - 계정 생성 및 JWT 쿠키 발급
     *
     * ✅ Service에서 받은 토큰으로 쿠키 설정
     */
    @PostMapping("/complete")
    fun complete(
        @Valid @RequestBody request: SignupCompleteRequest,
        @CookieValue("signup_token") signupToken: String,
        response: HttpServletResponse,
    ): ResponseEntity<MemberIdResponse> {
        // Service 호출 (토큰 받기)
        val result = signupFacadeService.completeSignup(signupToken, request)

        // ✅ Presentation 계층에서 쿠키 설정
        cookieManager.setAuthCookies(
            response = response,
            tokens = AuthTokens(
                accessToken = result.accessToken,
                refreshToken = result.refreshToken
            )
        )

        return ResponseEntity.ok(MemberIdResponse(memberId = result.memberId))
    }

    /**
     * 계정 복구 완료
     *
     * - 탈퇴/정지 계정의 재온보딩
     * - SignupCompleteRequest 재사용
     * - email / username 재입력 + 인증
     * - ACTIVE 기준 중복 체크
     * - 기존 memberId 유지
     *
     * ✅ Service에서 받은 토큰으로 쿠키 설정
     */
    @PostMapping("/recovery/complete")
    fun completeRecovery(
        @Valid @RequestBody request: SignupCompleteRequest,
        @CookieValue("recovery_token") recoveryToken: String,
        response: HttpServletResponse,
    ): ResponseEntity<MemberIdResponse> {

        // Recovery Service 호출
        val result = signupFacadeService.completeRecovery(
            recoveryToken = recoveryToken,
            request = request
        )

        // ✅ Presentation 계층에서 쿠키 설정
        cookieManager.setAuthCookies(
            response = response,
            tokens = AuthTokens(
                accessToken = result.accessToken,
                refreshToken = result.refreshToken
            )
        )

        return ResponseEntity.ok(MemberIdResponse(memberId = result.memberId))
    }

}

