package com.hirelog.api.member.application

import com.hirelog.api.auth.domain.OAuthUser
import com.hirelog.api.auth.infra.oauth.handler.OAuth2TokenIssuer
import com.hirelog.api.common.infra.redis.RedisService
import com.hirelog.api.common.infra.redis.dto.OAuthUserRedisDto
import com.hirelog.api.common.infra.redis.dto.OAuthUserRedisMapper
import com.hirelog.api.member.application.port.MemberQuery
import com.hirelog.api.member.presentation.dto.*
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * SignupFacadeService
 *
 * 책임:
 * - 가입/연동 유스케이스 전체 흐름 제어 (Orchestration)
 * - Redis 인증 세션 관리 및 토큰 발급 조율
 *
 * 설계 포인트:
 * - 모든 명칭을 'username'으로 통일하여 도메인-API 간 간극 제거
 * - 트랜잭션을 통해 DB 작업과 일회성 세션(Redis) 삭제의 원자성 보장
 */
@Service
class SignupFacadeService(
    private val memberQuery: MemberQuery,
    private val memberWriteService: MemberWriteService,
    private val redisService: RedisService,
    private val tokenIssuer: OAuth2TokenIssuer,
) {
    companion object {
        private const val SIGNUP_KEY_PREFIX = "SIGNUP:"
    }

    /**
     * 1. 이메일 가용성 체크
     * - 가입 토큰이 유효한지 먼저 확인 (Fail-fast)
     */
    @Transactional(readOnly = true)
    fun checkEmailAvailability(signupToken: String, email: String): CheckEmailResponse {
        validateSignupSession(signupToken)

        val member = memberQuery.findByEmail(email)

        return CheckEmailResponse(
            exists = member != null,
            username = member?.username // 변경: displayName -> username
        )
    }

    /**
     * 2. 기존 계정 연동
     * - Redis에서 OAuth 정보를 꺼내 기존 Member에 바인딩
     */
    @Transactional
    fun bindOAuthAccount(
        signupToken: String,
        request: BindRequest,
        response: HttpServletResponse
    ): Long {
        val oAuthUser = getOAuthUserOrThrow(signupToken)

        val member = memberWriteService.bindOAuthAccount(
            email = request.email,
            oAuthUser = oAuthUser
        )

        finalizeProcess(signupToken, member.id, response)
        return member.id
    }

    /**
     * 3. 신규 회원 가입 완료
     * - 추가 정보(직무, 경력 등)를 포함하여 신규 회원 생성
     */
    @Transactional
    fun completeSignup(
        signupToken: String,
        request: SignupCompleteRequest,
        response: HttpServletResponse
    ): Long {
        val oAuthUser = getOAuthUserOrThrow(signupToken)

        val member = memberWriteService.signupWithOAuth(
            email = request.email,
            username = request.username, // 변경: request의 username 사용
            oAuthUser = oAuthUser,
            currentPositionId = request.currentPositionId,
            careerYears = request.careerYears,
            summary = request.summary
        )

        finalizeProcess(signupToken, member.id, response)
        return member.id
    }

    /* ========================= 내부 헬퍼 로직 ========================= */

    /**
     * 프로세스 최종 확정
     * - 서비스 토큰(JWT) 발급 및 Redis 가입 임시 세션 제거
     */
    private fun finalizeProcess(token: String, memberId: Long, response: HttpServletResponse) {
        tokenIssuer.issueAccessAndRefresh(memberId, response)
        redisService.delete("$SIGNUP_KEY_PREFIX$token")
    }

    /**
     * Redis → OAuthUser 복원
     *
     * 흐름:
     * - Redis DTO 조회
     * - Domain 객체로 명시적 변환
     */
    private fun getOAuthUserOrThrow(token: String): OAuthUser {
        val dto = redisService.get(
            "$SIGNUP_KEY_PREFIX$token",
            OAuthUserRedisDto::class.java
        ) ?: throw IllegalArgumentException("유효하지 않거나 만료된 가입 세션입니다.")

        return OAuthUserRedisMapper.toDomain(dto)
    }

    /**
     * 단순 세션 유무 검증
     */
    private fun validateSignupSession(token: String) {
        if (!redisService.hasKey("$SIGNUP_KEY_PREFIX$token")) {
            throw IllegalArgumentException("잘못된 접근입니다. 가입 토큰이 유효하지 않습니다.")
        }
    }
}