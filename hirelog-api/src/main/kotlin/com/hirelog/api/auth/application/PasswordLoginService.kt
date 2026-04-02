package com.hirelog.api.auth.application

import com.hirelog.api.auth.application.dto.AuthTokens
import com.hirelog.api.member.application.port.MemberCommand
import com.hirelog.api.member.domain.MemberStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class PasswordLoginService(
    private val memberCommand: MemberCommand,
    private val tokenService: TokenService,
    private val passwordEncoder: PasswordEncoder,
) {
    fun login(email: String, password: String): AuthTokens {
        val normalizedEmail = email.trim().lowercase()
        val member = memberCommand.findByEmail(normalizedEmail)
            ?: throw IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.")

        if (member.status != MemberStatus.ACTIVE) {
            throw IllegalArgumentException("활성 상태의 계정이 아닙니다.")
        }

        val encodedPassword = member.password
            ?: throw IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.")

        if (!passwordEncoder.matches(password, encodedPassword)) {
            throw IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.")
        }

        return tokenService.generateAuthTokens(member.id)
    }
}
