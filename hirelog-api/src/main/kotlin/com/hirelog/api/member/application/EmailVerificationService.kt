package com.hirelog.api.member.application

import com.hirelog.api.common.exception.InvalidVerificationCodeException
import com.hirelog.api.common.exception.VerificationCodeExpiredException
import com.hirelog.api.common.infra.mail.EmailSender
import com.hirelog.api.common.infra.redis.RedisService
import com.hirelog.api.common.logging.log
import org.springframework.stereotype.Service
import java.security.SecureRandom
import java.time.Duration

/**
 * EmailVerificationService
 *
 * 책임:
 * - 이메일 인증코드 생성, 저장, 검증
 * - 인증 완료 상태 관리
 */
@Service
class EmailVerificationService(
    private val redisService: RedisService,
    private val emailSender: EmailSender,
) {
    companion object {
        private const val CODE_KEY_PREFIX = "EMAIL_CODE:"
        private const val VERIFIED_KEY_PREFIX = "EMAIL_VERIFIED:"
        private val CODE_TTL = Duration.ofMinutes(5)
        private val VERIFIED_TTL = Duration.ofMinutes(30)
        private const val CODE_LENGTH = 6
    }

    /**
     * 인증코드 생성 및 발송
     *
     * @return 생성된 6자리 인증코드
     */
    fun generateAndSave(signupToken: String, email: String): String {
        val code = generateCode()
        val key = "$CODE_KEY_PREFIX$signupToken"

        redisService.set(key, CodeData(email, code), CODE_TTL)

        emailSender.send(
            to = email,
            subject = "[HireLog] 이메일 인증코드",
            body = buildVerificationEmailBody(code)
        )

        log.info("[EMAIL_CODE_SENT] email={}", email)

        return code
    }


    private fun buildVerificationEmailBody(code: String): String {
        return """
            <div style="font-family: 'Apple SD Gothic Neo', 'Malgun Gothic', sans-serif; max-width: 600px; margin: 0 auto; padding: 40px 20px;">
                <h2 style="color: #333; margin-bottom: 30px;">HireLog 이메일 인증</h2>
                <p style="color: #666; font-size: 16px; line-height: 1.6;">
                    아래 인증코드를 입력해주세요.
                </p>
                <div style="background-color: #f5f5f5; padding: 20px; border-radius: 8px; text-align: center; margin: 30px 0;">
                    <span style="font-size: 32px; font-weight: bold; letter-spacing: 8px; color: #333;">$code</span>
                </div>
                <p style="color: #999; font-size: 14px;">
                    이 코드는 5분간 유효합니다.
                </p>
            </div>
        """.trimIndent()
    }

    /**
     * 인증코드 검증
     *
     * - 성공: 정상 종료
     * - 실패: 원인별 예외 발생
     */
    fun verifyOrThrow(token: String, email: String, code: String) {
        val key = "$CODE_KEY_PREFIX$token"

        val codeData = redisService.get(key, CodeData::class.java)
            ?: throw VerificationCodeExpiredException()

        // 이메일 또는 코드 불일치 (외부에는 동일한 실패로 노출)
        if (codeData.email != email || codeData.code != code) {
            throw InvalidVerificationCodeException()
        }

        // 인증 성공 → 코드 삭제
        redisService.delete(key)

        // 인증 완료 상태 저장 (세션 기준)
        redisService.set(
            key = "$VERIFIED_KEY_PREFIX$token",
            value = VerifiedData(email),
            duration = VERIFIED_TTL
        )
    }


    /**
     * 인증 완료 상태 확인
     */
    fun isVerified(signupToken: String, email: String): Boolean {
        val key = "$VERIFIED_KEY_PREFIX$signupToken"
        val data = redisService.get(key, VerifiedData::class.java)
            ?: return false

        return data.email == email
    }

    /**
     * 인증 완료 상태 삭제
     */
    fun clearVerified(signupToken: String) {
        redisService.delete("$VERIFIED_KEY_PREFIX$signupToken")
    }

    private fun generateCode(): String {
        val random = SecureRandom()
        return (1..CODE_LENGTH)
            .map { random.nextInt(10) }
            .joinToString("")
    }

    data class CodeData(
        val email: String,
        val code: String,
    )

    data class VerifiedData(
        val email: String,
    )
}
