package com.hirelog.api.member.application

import com.hirelog.api.auth.application.TokenService
import com.hirelog.api.common.logging.log
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
    private val recoverySessionService: RecoverySessionService
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
     * 인증코드 발송
     */
    fun sendVerificationCode(signupToken: String, email: String) {
        sessionService.validate(signupToken)

        if (!memberQuery.existsByEmail(email)) {
            throw IllegalArgumentException("존재하지 않는 이메일입니다.")
        }

        emailVerificationService.generateAndSave(signupToken, email)
    }

    /**
     * 인증코드 발급 (계정 복구용)
     */
    fun sendRecoveryVerificationCode(recoveryToken: String, email: String) {
        recoverySessionService.validate(recoveryToken)

        if (memberQuery.existsActiveByEmail(email)) {
            throw IllegalArgumentException("이미 사용 중인 이메일입니다.")
        }

        emailVerificationService.generateAndSave(recoveryToken, email)
    }

    /**
     * 인증코드 검증
     */
    fun verifyCode(signupToken: String, email: String, code: String): VerifyCodeResponse {
        sessionService.validate(signupToken)

        val verified = emailVerificationService.verify(signupToken, email, code)

        return VerifyCodeResponse(verified = verified)
    }

    fun verifyRecoveryCode(
        recoveryToken: String,
        email: String,
        code: String
    ): VerifyCodeResponse {
        recoverySessionService.validate(recoveryToken)

        val verified = emailVerificationService.verify(
            token = recoveryToken,
            email = email,
            code = code
        )

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
        val tokens = tokenService.generateAuthTokens(member.id)

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
        val tokens = tokenService.generateAuthTokens(member.id)

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
     * 계정 복구 완료
     *
     * 정책:
     * - SignupCompleteRequest 재사용
     * - email / username 모두 새로 입력 + 인증
     * - ACTIVE 기준 중복 체크
     * - 기존 memberId 유지
     * - 상태 ACTIVE 전환
     */
    @Transactional
    fun completeRecovery(
        recoveryToken: String,
        request: SignupCompleteRequest
    ): SignupResult {

        // 1. Recovery 세션 검증
        recoverySessionService.validate(recoveryToken)

        // 2. 이메일 인증 여부 확인 (Signup과 동일)
        validateEmailVerified(recoveryToken, request.email)

        // 3. 복구 대상 memberId 조회
        val memberId = recoverySessionService.getMemberId(recoveryToken)

        // 4. Recovery 수행
        val member = memberWriteService.recoveryAccount(
            memberId = memberId,
            email = request.email,
            username = request.username,
            currentPositionId = request.currentPositionId,
            careerYears = request.careerYears,
            summary = request.summary
        )

        // 5. 토큰 발급
        val tokens = tokenService.generateAuthTokens(member.id)

        // 6. 세션 정리
        recoverySessionService.clear(recoveryToken)
        emailVerificationService.clearVerified(recoveryToken)

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