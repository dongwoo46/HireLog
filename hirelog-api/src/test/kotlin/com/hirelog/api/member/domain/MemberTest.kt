package com.hirelog.api.member.domain

import com.hirelog.api.auth.domain.OAuth2Provider
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.*

@DisplayName("Member 도메인 테스트")
class MemberTest {

    @Nested
    @DisplayName("createByOAuth 팩토리는")
    inner class CreateByOAuthTest {

        @Test
        @DisplayName("정상 값으로 Member를 생성하고 OAuth 계정을 연결한다")
        fun shouldCreateMemberWithOAuthAccount() {
            val member = Member.createByOAuth(
                email = "user@example.com",
                username = "toss_user",
                provider = OAuth2Provider.GOOGLE,
                providerUserId = "google-123"
            )

            assertThat(member.email).isEqualTo("user@example.com")
            assertThat(member.username).isEqualTo("toss_user")
            assertThat(member.role).isEqualTo(MemberRole.USER)
            assertThat(member.status).isEqualTo(MemberStatus.ACTIVE)
            assertThat(member.hasOAuthAccount(OAuth2Provider.GOOGLE)).isTrue()
        }

        @Test
        @DisplayName("blank email이면 예외를 던진다")
        fun shouldThrowWhenEmailIsBlank() {
            assertThatThrownBy {
                Member.createByOAuth(
                    email = "",
                    username = "user",
                    provider = OAuth2Provider.GOOGLE,
                    providerUserId = "google-123"
                )
            }.isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        @DisplayName("음수 careerYears이면 예외를 던진다")
        fun shouldThrowWhenCareerYearsIsNegative() {
            assertThatThrownBy {
                Member.createByOAuth(
                    email = "user@example.com",
                    username = "user",
                    provider = OAuth2Provider.GOOGLE,
                    providerUserId = "google-123",
                    careerYears = -1
                )
            }.isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        @DisplayName("summary가 1000자를 초과하면 예외를 던진다")
        fun shouldThrowWhenSummaryTooLong() {
            assertThatThrownBy {
                Member.createByOAuth(
                    email = "user@example.com",
                    username = "user",
                    provider = OAuth2Provider.GOOGLE,
                    providerUserId = "google-123",
                    summary = "a".repeat(1001)
                )
            }.isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    @Nested
    @DisplayName("linkOAuthAccount는")
    inner class LinkOAuthAccountTest {

        @Test
        @DisplayName("동일 provider를 중복 연결하면 예외를 던진다")
        fun shouldThrowWhenDuplicateProvider() {
            val member = Member.createByOAuth(
                email = "user@example.com",
                username = "user",
                provider = OAuth2Provider.GOOGLE,
                providerUserId = "google-123"
            )

            assertThatThrownBy {
                member.linkOAuthAccount(OAuth2Provider.GOOGLE, "google-456")
            }.isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        @DisplayName("다른 provider는 추가 연결할 수 있다")
        fun shouldLinkDifferentProvider() {
            val member = Member.createByOAuth(
                email = "user@example.com",
                username = "user",
                provider = OAuth2Provider.GOOGLE,
                providerUserId = "google-123"
            )

            member.linkOAuthAccount(OAuth2Provider.KAKAO, "kakao-456")

            assertThat(member.hasOAuthAccount(OAuth2Provider.KAKAO)).isTrue()
        }
    }

    @Nested
    @DisplayName("updateDisplayName는")
    inner class UpdateDisplayNameTest {

        @Test
        @DisplayName("username을 변경한다")
        fun shouldChangeUsername() {
            val member = Member.createByOAuth(
                email = "user@example.com",
                username = "old_name",
                provider = OAuth2Provider.GOOGLE,
                providerUserId = "google-123"
            )

            member.updateDisplayName("new_name")

            assertThat(member.username).isEqualTo("new_name")
        }
    }

    @Nested
    @DisplayName("updateProfile는")
    inner class UpdateProfileTest {

        @Test
        @DisplayName("프로필 필드를 변경한다")
        fun shouldUpdateProfile() {
            val member = Member.createByOAuth(
                email = "user@example.com",
                username = "user",
                provider = OAuth2Provider.GOOGLE,
                providerUserId = "google-123"
            )

            member.updateProfile(currentPositionId = 10L, careerYears = 3, summary = "백엔드 개발자")

            assertThat(member.currentPositionId).isEqualTo(10L)
            assertThat(member.careerYears).isEqualTo(3)
            assertThat(member.summary).isEqualTo("백엔드 개발자")
        }

        @Test
        @DisplayName("음수 careerYears이면 예외를 던진다")
        fun shouldThrowWhenCareerYearsNegative() {
            val member = Member.createByOAuth(
                email = "user@example.com",
                username = "user",
                provider = OAuth2Provider.GOOGLE,
                providerUserId = "google-123"
            )

            assertThatThrownBy {
                member.updateProfile(null, careerYears = -1, null)
            }.isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    @Nested
    @DisplayName("생명주기 메서드는")
    inner class LifecycleTest {

        @Test
        @DisplayName("suspend() → ACTIVE에서만 SUSPENDED로 전이한다")
        fun shouldSuspendFromActive() {
            val member = Member.createByOAuth("u@e.com", "u", OAuth2Provider.GOOGLE, "g-1")
            member.suspend()
            assertThat(member.status).isEqualTo(MemberStatus.SUSPENDED)
        }

        @Test
        @DisplayName("suspend() → 이미 SUSPENDED이면 예외를 던진다")
        fun shouldThrowSuspendWhenNotActive() {
            val member = Member.createByOAuth("u@e.com", "u", OAuth2Provider.GOOGLE, "g-1")
            member.suspend()
            assertThatThrownBy { member.suspend() }.isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        @DisplayName("activate() → SUSPENDED에서 ACTIVE로 전이한다")
        fun shouldActivateFromSuspended() {
            val member = Member.createByOAuth("u@e.com", "u", OAuth2Provider.GOOGLE, "g-1")
            member.suspend()
            member.activate()
            assertThat(member.status).isEqualTo(MemberStatus.ACTIVE)
        }

        @Test
        @DisplayName("activate() → DELETED에서 ACTIVE로 전이한다")
        fun shouldActivateFromDeleted() {
            val member = Member.createByOAuth("u@e.com", "u", OAuth2Provider.GOOGLE, "g-1")
            member.softDelete()
            member.activate()
            assertThat(member.status).isEqualTo(MemberStatus.ACTIVE)
        }

        @Test
        @DisplayName("activate() → 이미 ACTIVE이면 예외를 던진다")
        fun shouldThrowActivateWhenActive() {
            val member = Member.createByOAuth("u@e.com", "u", OAuth2Provider.GOOGLE, "g-1")
            assertThatThrownBy { member.activate() }.isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        @DisplayName("softDelete() → ACTIVE에서 DELETED로 전이한다")
        fun shouldSoftDeleteFromActive() {
            val member = Member.createByOAuth("u@e.com", "u", OAuth2Provider.GOOGLE, "g-1")
            member.softDelete()
            assertThat(member.status).isEqualTo(MemberStatus.DELETED)
        }

        @Test
        @DisplayName("softDelete() → 이미 DELETED이면 예외를 던진다")
        fun shouldThrowSoftDeleteWhenAlreadyDeleted() {
            val member = Member.createByOAuth("u@e.com", "u", OAuth2Provider.GOOGLE, "g-1")
            member.softDelete()
            assertThatThrownBy { member.softDelete() }.isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    @Nested
    @DisplayName("권한 메서드는")
    inner class RoleTest {

        @Test
        @DisplayName("grantAdmin() → ACTIVE 상태에서 ADMIN 권한을 부여한다")
        fun shouldGrantAdminWhenActive() {
            val member = Member.createByOAuth("u@e.com", "u", OAuth2Provider.GOOGLE, "g-1")
            member.grantAdmin()
            assertThat(member.isAdmin()).isTrue()
            assertThat(member.role).isEqualTo(MemberRole.ADMIN)
        }

        @Test
        @DisplayName("grantAdmin() → SUSPENDED 상태에서는 예외를 던진다")
        fun shouldThrowGrantAdminWhenSuspended() {
            val member = Member.createByOAuth("u@e.com", "u", OAuth2Provider.GOOGLE, "g-1")
            member.suspend()
            assertThatThrownBy { member.grantAdmin() }.isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        @DisplayName("isAdmin() → USER 역할이면 false를 반환한다")
        fun shouldReturnFalseForUser() {
            val member = Member.createByOAuth("u@e.com", "u", OAuth2Provider.GOOGLE, "g-1")
            assertThat(member.isAdmin()).isFalse()
        }
    }
}
