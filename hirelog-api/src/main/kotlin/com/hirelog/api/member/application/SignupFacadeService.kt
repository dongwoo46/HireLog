package com.hirelog.api.member.application

import com.hirelog.api.auth.application.TokenService
import com.hirelog.api.member.application.dto.SignupResult
import com.hirelog.api.member.application.port.MemberQuery
import com.hirelog.api.member.presentation.dto.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * SignupFacadeService
 *
 * 책임:
 * - 가입/연동 유스케이스 오케스트레이션
 *
 * 설계 포인트:
 * - 세션 관리는 SignupSessionService로 위임
 * - 이메일 인증은 EmailVerificationService로 위임
 * - 회원 생성/연동은 MemberWriteService로 위임
 * - 토큰 생성은 TokenService로 위임
 * - ✅ HttpServletResponse 제거 (Presentation 계층 의존 제거)
 */
@Service
class SignupFacadeService(
    private val sessionService: SignupSessionService,
    private val emailVerificationService: EmailVerificationService,
    private val memberQuery: MemberQuery,
    private val memberWriteService: MemberWriteService,
    private val tokenService: TokenService,
) {

    /**
     * 이메일 중복 체크 및 인증코드 발송
     *
     * - 중복 아님: 인증코드 발송, exists=false
     * - 중복: exists=true (프론트에서 연결 여부 선택)
     */
    @Transactional(readOnly = true)
    fun checkEmailAvailability(signupToken: String, email: String): CheckEmailResponse {
        sessionService.validate(signupToken)

        val exists = memberQuery.existsByEmail(email)

        if (!exists) {
            emailVerificationService.generateAndSave(signupToken, email)
        }

        return CheckEmailResponse(exists = exists)
    }

    /**
     * 인증코드 발송 (기존 계정 연결 선택 시)
     */
    fun sendVerificationCode(signupToken: String, email: String) {
        sessionService.validate(signupToken)

        if (!memberQuery.existsByEmail(email)) {
            throw IllegalArgumentException("존재하지 않는 이메일입니다.")
        }

        emailVerificationService.generateAndSave(signupToken, email)
    }

    /**
     * 인증코드 검증
     */
    fun verifyCode(signupToken: String, email: String, code: String): VerifyCodeResponse {
        sessionService.validate(signupToken)

        val verified = emailVerificationService.verify(signupToken, email, code)

        return VerifyCodeResponse(verified = verified)
    }

    /**
     * 기존 계정 연동
     *
     * ✅ HttpServletResponse 제거, SignupResult 반환
     */
    @Transactional
    fun bindOAuthAccount(
        signupToken: String,
        request: BindRequest
    ): SignupResult {
        validateEmailVerified(signupToken, request.email)

        val oAuthUser = sessionService.getOAuthUser(signupToken)

        val member = memberWriteService.bindOAuthAccount(
            email = request.email,
            oAuthUser = oAuthUser
        )

        // ✅ 토큰 생성 (쿠키 설정은 안 함!)
        val tokens = tokenService.generateAuthTokens(member.id, member.role.name)

        // 세션 정리
        sessionService.clear(signupToken)
        emailVerificationService.clearVerified(signupToken)

        return SignupResult(
            memberId = member.id,
            accessToken = tokens.accessToken,
            refreshToken = tokens.refreshToken
        )
    }

    /**
     * 신규 회원 가입 완료
     *
     * ✅ HttpServletResponse 제거, SignupResult 반환
     */
    @Transactional
    fun completeSignup(
        signupToken: String,
        request: SignupCompleteRequest
    ): SignupResult {
        validateEmailVerified(signupToken, request.email)

        val oAuthUser = sessionService.getOAuthUser(signupToken)

        val member = memberWriteService.signupWithOAuth(
            email = request.email,
            username = request.username,
            oAuthUser = oAuthUser,
            currentPositionId = request.currentPositionId,
            careerYears = request.careerYears,
            summary = request.summary
        )

        // ✅ 토큰 생성 (쿠키 설정은 안 함!)
        val tokens = tokenService.generateAuthTokens(member.id, member.role.name)

        // 세션 정리
        sessionService.clear(signupToken)
        emailVerificationService.clearVerified(signupToken)

        return SignupResult(
            memberId = member.id,
            accessToken = tokens.accessToken,
            refreshToken = tokens.refreshToken
        )
    }

    /* ========================= Private ========================= */

    private fun validateEmailVerified(signupToken: String, email: String) {
        if (!emailVerificationService.isVerified(signupToken, email)) {
            throw IllegalArgumentException("이메일 인증이 완료되지 않았습니다.")
        }
    }
}