package com.hirelog.api.auth.application

import com.hirelog.api.auth.domain.OAuth2LoginResult
import com.hirelog.api.auth.domain.OAuth2Provider
import com.hirelog.api.auth.domain.OAuthUser
import com.hirelog.api.auth.infra.oauth.handler.OAuthLoginHandler
import com.hirelog.api.member.application.port.MemberOAuthAccountQuery
import com.hirelog.api.member.domain.Member
import com.hirelog.api.member.domain.MemberOAuthAccount
import com.hirelog.api.member.domain.MemberRole
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("OAuthLoginService 테스트")
class OAuthLoginHandlerTest {

    private val memberOAuthAccountQuery: MemberOAuthAccountQuery = mockk()

    private val service = OAuthLoginHandler(memberOAuthAccountQuery)

    @Test
    @DisplayName("기존 회원 로그인: OAuth 계정이 존재하면 ExistingUser 결과를 반환한다")
    fun loginProcess_existing_user() {
        // Arrange
        val oAuthUser = OAuthUser(
            provider = OAuth2Provider.GOOGLE,
            providerUserId = "12345",
            email = "test@example.com"
        )

        val member = mockk<Member>()
        every { member.id } returns 10L
        every { member.role } returns MemberRole.USER

        val account = mockk<MemberOAuthAccount>()
        every { account.member } returns member

        every {
            memberOAuthAccountQuery.findByProviderAndProviderUserId(
                OAuth2Provider.GOOGLE,
                "12345"
            )
        } returns account

        // Act
        val result = service.loginProcess(oAuthUser)

        // Assert
        assertInstanceOf(OAuth2LoginResult.ExistingUser::class.java, result)

        val existingUser = result as OAuth2LoginResult.ExistingUser
        assertEquals(10L, existingUser.memberId)
        assertEquals("USER", existingUser.role)
    }

    @Test
    @DisplayName("신규 회원 로그인: OAuth 계정이 없으면 NewUser 결과를 반환한다")
    fun loginProcess_new_user() {
        // Arrange
        val oAuthUser = OAuthUser(
            provider = OAuth2Provider.KAKAO,
            providerUserId = "98765",
            email = "new@example.com"
        )

        every {
            memberOAuthAccountQuery.findByProviderAndProviderUserId(any(), any())
        } returns null

        // Act
        val result = service.loginProcess(oAuthUser)

        // Assert
        assertInstanceOf(OAuth2LoginResult.NewUser::class.java, result)

        val newUser = result as OAuth2LoginResult.NewUser
        assertEquals("new@example.com", newUser.email)
        assertEquals(OAuth2Provider.KAKAO, newUser.provider)
        assertEquals("98765", newUser.providerUserId)
    }
}
