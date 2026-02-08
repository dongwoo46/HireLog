package com.hirelog.api.member.domain

import com.hirelog.api.auth.domain.OAuth2Provider
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Member 도메인 테스트")
class MemberTest {

    @Test
    @DisplayName("OAuth 정보로 회원 생성 시 올바르게 생성되어야 한다")
    fun createByOAuth() {
        // Arrange
        val email = "test@example.com"
        val username = "TestUser"
        val provider = OAuth2Provider.GOOGLE
        val providerUserId = "12345"

        // Act
        val member = Member.createByOAuth(email, username, provider, providerUserId)

        // Assert
        assertEquals(email, member.email)
        assertEquals(username, member.username)
        assertEquals(MemberRole.USER, member.role)
        assertEquals(MemberStatus.ACTIVE, member.status)
        assertTrue(member.hasOAuthAccount(provider))
    }

    @Test
    @DisplayName("OAuth 계정 연결 시 계정이 추가되어야 한다")
    fun linkOAuthAccount() {
        // Arrange
        val member = Member.createByOAuth("test@example.com", "User", OAuth2Provider.GOOGLE, "1")

        // Act
        member.linkOAuthAccount(OAuth2Provider.KAKAO, "2")

        // Assert
        assertTrue(member.hasOAuthAccount(OAuth2Provider.KAKAO))
    }

    @Test
    @DisplayName("이미 연결된 Provider 재연결 시 예외가 발생해야 한다")
    fun linkOAuthAccount_fail_duplicate() {
        // Arrange
        val member = Member.createByOAuth("test@example.com", "User", OAuth2Provider.GOOGLE, "1")

        // Act & Assert
        assertThrows(IllegalArgumentException::class.java) {
            member.linkOAuthAccount(OAuth2Provider.GOOGLE, "2")
        }
    }

    @Test
    @DisplayName("프로필 업데이트 시 값이 변경되어야 한다")
    fun updateProfile() {
        // Arrange
        val member = Member.createByOAuth("test@example.com", "User", OAuth2Provider.GOOGLE, "1")
        val newPositionId = 10L
        val newCareerYears = 5
        val newSummary = "Updated Summary"

        // Act
        member.updateProfile(newPositionId, newCareerYears, newSummary)

        // Assert
        assertEquals(newPositionId, member.currentPositionId)
        assertEquals(newCareerYears, member.careerYears)
        assertEquals(newSummary, member.summary)
    }

    @Test
    @DisplayName("계정 정지 및 활성화 상태 변경 테스트")
    fun suspend_and_activate() {
        // Arrange
        val member = Member.createByOAuth("test@example.com", "User", OAuth2Provider.GOOGLE, "1")

        // Act (Suspend)
        member.suspend()
        assertEquals(MemberStatus.SUSPENDED, member.status)

        // Act (Activate)
        member.activate()
        assertEquals(MemberStatus.ACTIVE, member.status)
    }

    @Test
    @DisplayName("계정 탈퇴(Soft Delete) 시 status가 DELETED로 변경되어야 한다")
    fun softDelete() {
        // Arrange
        val member = Member.createByOAuth("test@example.com", "User", OAuth2Provider.GOOGLE, "1")

        // Act
        member.softDelete()

        // Assert
        assertEquals(MemberStatus.DELETED, member.status)
    }
}
