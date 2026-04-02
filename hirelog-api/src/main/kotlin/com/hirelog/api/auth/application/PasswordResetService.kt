package com.hirelog.api.auth.application

import com.hirelog.api.member.application.EmailVerificationService
import com.hirelog.api.member.application.MemberWriteService
import com.hirelog.api.member.application.port.MemberQuery
import org.springframework.stereotype.Service

@Service
class PasswordResetService(
    private val emailVerificationService: EmailVerificationService,
    private val memberQuery: MemberQuery,
    private val memberWriteService: MemberWriteService,
) {
    fun sendCode(email: String) {
        val normalizedEmail = email.trim().lowercase()
        if (!memberQuery.existsActiveByEmail(normalizedEmail)) {
            throw IllegalArgumentException("존재하지 않는 이메일입니다.")
        }

        emailVerificationService.generateAndSave(
            signupToken = verificationKey(normalizedEmail),
            email = normalizedEmail
        )
    }

    fun verifyCode(email: String, code: String) {
        val normalizedEmail = email.trim().lowercase()
        emailVerificationService.verifyOrThrow(
            token = verificationKey(normalizedEmail),
            email = normalizedEmail,
            code = code
        )
    }

    fun resetPassword(email: String, newPassword: String) {
        val normalizedEmail = email.trim().lowercase()
        val key = verificationKey(normalizedEmail)

        if (!emailVerificationService.isVerified(key, normalizedEmail)) {
            throw IllegalArgumentException("이메일 인증이 완료되지 않았습니다.")
        }

        memberWriteService.changePasswordByEmail(normalizedEmail, newPassword)
        emailVerificationService.clearVerified(key)
    }

    private fun verificationKey(email: String): String = "PASSWORD_RESET:$email"
}

