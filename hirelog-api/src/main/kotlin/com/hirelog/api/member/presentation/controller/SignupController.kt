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
 * - 클라이언트 요청 바인딩/유효성 검증 (@Valid)
 * - signup/recovery 토큰 전달
 * - 비즈니스 결과에 따른 HTTP 응답 제어
 *
 * 설계 포인트:
 * - Thin Controller: 실제 로직은 SignupFacadeService 위임
 * - 쿠키 설정은 CookieManager 사용 (Presentation 책임)
 * - 일반 회원가입/소셜 회원가입은 동일 엔드포인트에서 signup_token 유무로 분기
 */
@RestController
@RequestMapping("/api/auth/signup")
class SignupController(
    private val signupFacadeService: SignupFacadeService,
    private val cookieManager: CookieManager,
) {

    /**
     * 이메일 중복 체크 및 인증코드 발송
     *
     * - signup_token 존재: 소셜 가입 플로우
     * - signup_token 없음: 일반 가입 플로우
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
     * 인증코드 발송 (재전송)
     *
     * - signup_token 존재: 소셜 가입 세션 기준
     * - signup_token 없음: 일반 가입 이메일 기준
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
     *
     * - signup_token 존재: 소셜 가입 세션 기준
     * - signup_token 없음: 일반 가입 이메일 기준
     */
    @PostMapping("/verify-code")
    fun verifyCode(
        @Valid @RequestBody request: VerifyCodeRequest,
        @CookieValue("signup_token") signupToken: String,
    ): ResponseEntity<VerifyCodeResponse> {
        signupFacadeService.verifyCode(signupToken, request.email, request.code)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/recovery/verify-code")
    fun verifyRecoveryCode(
        @Valid @RequestBody request: VerifyCodeRequest,
        @CookieValue("recovery_token") recoveryToken: String,
    ): ResponseEntity<VerifyCodeResponse> {
        signupFacadeService.verifyRecoveryCode(
            recoveryToken = recoveryToken,
            email = request.email,
            code = request.code
        )
        return ResponseEntity.noContent().build()
    }

    /**
     * 기존 회원 계정 연동 (소셜 전용)
     */
    @PostMapping("/bind")
    fun bind(
        @Valid @RequestBody request: BindRequest,
        @CookieValue("signup_token") signupToken: String,
        response: HttpServletResponse,
    ): ResponseEntity<MemberIdResponse> {
        val result = signupFacadeService.bindOAuthAccount(signupToken, request)

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
     * 회원가입 완료
     *
     * - signup_token 존재: 소셜 가입 완료
     * - signup_token 없음: 일반 가입 완료 (password 필요)
     */
    @PostMapping("/complete")
    fun complete(
        @Valid @RequestBody request: SignupCompleteRequest,
        @CookieValue("signup_token") signupToken: String,
        response: HttpServletResponse,
    ): ResponseEntity<MemberIdResponse> {
        val result = signupFacadeService.completeSignup(signupToken, request)

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
     * 일반 회원가입: 이메일 중복 체크 + 인증코드 발송
     */
    @PostMapping("/general/check-email")
    fun checkGeneralEmail(
        @Valid @RequestBody request: CheckEmailRequest,
    ): ResponseEntity<CheckEmailResponse> {
        val response = signupFacadeService.checkGeneralEmailAvailability(request.email)
        return ResponseEntity.ok(response)
    }

    /**
     * 일반 회원가입용 닉네임 중복 체크
     */
    @PostMapping("/general/check-username")
    fun checkGeneralUsername(
        @Valid @RequestBody request: CheckUsernameRequest,
    ): ResponseEntity<CheckUsernameResponse> {
        val response = signupFacadeService.checkGeneralUsernameAvailability(request.username)
        return ResponseEntity.ok(response)
    }

    /**
     * 일반 회원가입: 인증코드 재발송
     */
    @PostMapping("/general/send-code")
    fun sendGeneralCode(
        @Valid @RequestBody request: SendCodeRequest,
    ): ResponseEntity<Void> {
        signupFacadeService.sendGeneralVerificationCode(request.email)
        return ResponseEntity.ok().build()
    }

    /**
     * 일반 회원가입: 인증코드 검증
     */
    @PostMapping("/general/verify-code")
    fun verifyGeneralCode(
        @Valid @RequestBody request: VerifyCodeRequest,
    ): ResponseEntity<Void> {
        signupFacadeService.verifyGeneralCode(request.email, request.code)
        return ResponseEntity.noContent().build()
    }

    /**
     * 일반 회원가입: 최종 가입 완료
     */
    @PostMapping("/general/complete")
    fun completeGeneralSignup(
        @Valid @RequestBody request: SignupCompleteRequest,
        response: HttpServletResponse,
    ): ResponseEntity<MemberIdResponse> {
        val result = signupFacadeService.completeGeneralSignup(request)

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
     */
    @PostMapping("/recovery/complete")
    fun completeRecovery(
        @Valid @RequestBody request: SignupCompleteRequest,
        @CookieValue("recovery_token") recoveryToken: String,
        response: HttpServletResponse,
    ): ResponseEntity<MemberIdResponse> {
        val result = signupFacadeService.completeRecovery(
            recoveryToken = recoveryToken,
            request = request
        )

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
