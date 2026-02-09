package com.hirelog.api.member.application

import com.hirelog.api.auth.domain.OAuth2Provider
import com.hirelog.api.auth.domain.OAuthUser
import com.hirelog.api.common.config.properties.AdminProperties
import com.hirelog.api.common.exception.EntityNotFoundException
import com.hirelog.api.member.application.port.MemberCommand
import com.hirelog.api.member.application.port.MemberQuery
import com.hirelog.api.member.domain.Member
import com.hirelog.api.member.domain.MemberRole
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("MemberWriteService 테스트")
class MemberWriteServiceTest {

    private val memberCommand: MemberCommand = mockk()
    private val memberQuery: MemberQuery = mockk()
    private val adminProperties: AdminProperties = mockk(relaxed = true)

    private val service = MemberWriteService(memberCommand, memberQuery, adminProperties)

    @Test
    @DisplayName("OAuth 회원가입 성공: 이메일/Username 중복이 없으면 회원을 생성하고 저장해야 한다")
    fun signupWithOAuth_success() {
        // Arrange
        val email = "new@example.com"
        val username = "NewUser"
        val oAuthUser = OAuthUser(
            provider = OAuth2Provider.GOOGLE,
            providerUserId = "123",
            email = email,
        )

        every { memberQuery.existsByEmail(email) } returns false
        every { memberQuery.existsByUsername(username) } returns false
        every { adminProperties.isAdmin(email) } returns false
        every { memberCommand.save(any()) } answers { firstArg() }

        // Act
        val member = service.signupWithOAuth(email, username, oAuthUser)

        // Assert
        assertEquals(email, member.email)
        assertEquals(username, member.username)
        assertEquals(MemberRole.USER, member.role)

        verify(exactly = 1) { memberCommand.save(any()) }
    }

    @Test
    @DisplayName("OAuth 회원가입 시 Admin 이메일이면 ADMIN 권한이 부여되어야 한다")
    fun signupWithOAuth_admin() {
        // Arrange
        val email = "admin@hirelog.com"
        val oAuthUser = OAuthUser(
            provider = OAuth2Provider.GOOGLE,
            providerUserId = "admin-123",
            email = email,
        )

        every { memberQuery.existsByEmail(email) } returns false
        every { memberQuery.existsByUsername(any()) } returns false
        every { adminProperties.isAdmin(email) } returns true
        every { memberCommand.save(any()) } answers { firstArg() }

        // Act
        val member = service.signupWithOAuth(email, "AdminUser", oAuthUser)

        // Assert
        assertEquals(MemberRole.ADMIN, member.role)
    }

    @Test
    @DisplayName("계정 정지(Suspend) 성공")
    fun suspend_success() {
        // Arrange
        val memberId = 1L
        val member = mockk<Member>(relaxed = true)

        every { memberCommand.findById(memberId) } returns member

        // Act
        service.suspend(memberId)

        // Assert
        verify(exactly = 1) { member.suspend() }
    }

    @Test
    @DisplayName("존재하지 않는 회원 정지 시 EntityNotFoundException 발생")
    fun suspend_fail_not_found() {
        // Arrange
        val memberId = 999L
        every { memberCommand.findById(memberId) } returns null

        // Act & Assert
        assertThrows(EntityNotFoundException::class.java) {
            service.suspend(memberId)
        }
    }
}
