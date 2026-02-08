package com.hirelog.api.member.infra.persistence.jpa.repository

import com.hirelog.api.auth.domain.OAuth2Provider
import com.hirelog.api.member.domain.Member
import com.hirelog.api.member.domain.MemberStatus
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager

@DataJpaTest
@DisplayName("MemberJpaRepository")
class MemberJpaRepositoryTest @Autowired constructor(
    private val memberRepository: MemberJpaRepository,
    private val entityManager: TestEntityManager
) {

    @BeforeEach
    fun setUp() {
        entityManager.clear()
    }

    /* =========================
     * Test Fixtures
     * ========================= */

    private fun createMember(
        email: String = "test@example.com",
        username: String = "testuser",
        provider: OAuth2Provider = OAuth2Provider.GOOGLE,
        providerUserId: String = java.util.UUID.randomUUID().toString(), // OAuth ID 중복 방지
        currentPositionId: Long? = null,
        careerYears: Int? = null,
        summary: String? = null
    ): Member {
        return Member.createByOAuth(
            email = email,
            username = username,
            provider = provider,
            providerUserId = providerUserId,
            currentPositionId = currentPositionId,
            careerYears = careerYears,
            summary = summary
        )
    }

    private fun save(member: Member): Member {
        val saved = memberRepository.save(member)
        entityManager.flush()
        entityManager.clear()
        return saved
    }

    /* =========================
     * findByEmail
     * ========================= */

    @Nested
    @DisplayName("findByEmail")
    inner class FindByEmail {

        @Test
        fun `존재하는 이메일로 조회하면 회원 반환`() {
            save(createMember(email = "john@example.com", username = "john"))

            val found = memberRepository.findByEmail("john@example.com")

            assertNotNull(found)
            assertEquals("john@example.com", found!!.email)
            assertEquals("john", found.username)
        }

        @Test
        fun `존재하지 않는 이메일이면 null 반환`() {
            val found = memberRepository.findByEmail("none@example.com")
            assertNull(found)
        }

        @Test
        fun `이메일은 대소문자를 구분한다`() {
            save(createMember(email = "case@example.com"))

            val found = memberRepository.findByEmail("CASE@example.com")

            assertNull(found)
        }
    }

    /* =========================
     * findByUsername
     * ========================= */

    @Nested
    @DisplayName("findByUsername")
    inner class FindByUsername {

        @Test
        fun `존재하는 username으로 조회하면 회원 반환`() {
            save(createMember(username = "alice"))

            val found = memberRepository.findByUsername("alice")

            assertNotNull(found)
            assertEquals("alice", found!!.username)
        }

        @Test
        fun `존재하지 않는 username이면 null 반환`() {
            val found = memberRepository.findByUsername("ghost")
            assertNull(found)
        }
    }

    /* =========================
     * existsByEmail
     * ========================= */

    @Nested
    @DisplayName("existsByEmail")
    inner class ExistsByEmail {

        @Test
        fun `존재하면 true`() {
            save(createMember(email = "exists@example.com"))

            assertTrue(memberRepository.existsByEmail("exists@example.com"))
        }

        @Test
        fun `존재하지 않으면 false`() {
            assertFalse(memberRepository.existsByEmail("none@example.com"))
        }
    }

    /* =========================
     * existsByUsername
     * ========================= */

    @Nested
    @DisplayName("existsByUsername")
    inner class ExistsByUsername {

        @Test
        fun `존재하면 true`() {
            save(createMember(username = "bob"))

            assertTrue(memberRepository.existsByUsername("bob"))
        }

        @Test
        fun `존재하지 않으면 false`() {
            assertFalse(memberRepository.existsByUsername("nobody"))
        }
    }

    /* =========================
     * existsByIdAndStatus
     * ========================= */

    @Nested
    @DisplayName("existsByIdAndStatus")
    inner class ExistsByIdAndStatus {

        @Test
        fun `ACTIVE 상태 회원이면 true`() {
            val saved = save(createMember())

            assertTrue(
                memberRepository.existsByIdAndStatus(saved.id, MemberStatus.ACTIVE)
            )
        }

        @Test
        fun `상태가 다르면 false`() {
            val saved = save(createMember())

            assertFalse(
                memberRepository.existsByIdAndStatus(saved.id, MemberStatus.SUSPENDED)
            )
        }

        @Test
        fun `존재하지 않는 ID면 false`() {
            assertFalse(
                memberRepository.existsByIdAndStatus(9999L, MemberStatus.ACTIVE)
            )
        }

        @Test
        fun `ACTIVE 와 SUSPENDED 구분`() {
            val active = save(
                createMember(
                    email = "active@example.com",
                    username = "active-user"
                )
            )

            val suspended = save(
                createMember(
                    email = "suspended@example.com",
                    username = "suspended-user"
                )
            )

            suspended.suspend()
            memberRepository.save(suspended)
            entityManager.flush()
            entityManager.clear()

            assertTrue(memberRepository.existsByIdAndStatus(active.id, MemberStatus.ACTIVE))
            assertFalse(memberRepository.existsByIdAndStatus(suspended.id, MemberStatus.ACTIVE))
            assertTrue(memberRepository.existsByIdAndStatus(suspended.id, MemberStatus.SUSPENDED))
        }

    }

    /* =========================
     * Integration Scenarios
     * ========================= */

    @Nested
    @DisplayName("복합 시나리오")
    inner class IntegrationScenarios {

        @Test
        fun `회원 저장 후 모든 조회 API로 확인`() {
            val saved = save(
                createMember(
                    email = "full@example.com",
                    username = "fulluser"
                )
            )

            assertNotNull(memberRepository.findByEmail("full@example.com"))
            assertNotNull(memberRepository.findByUsername("fulluser"))
            assertTrue(memberRepository.existsByEmail("full@example.com"))
            assertTrue(memberRepository.existsByUsername("fulluser"))
            assertTrue(memberRepository.existsByIdAndStatus(saved.id, MemberStatus.ACTIVE))
        }

        @Test
        fun `displayName 변경 후 조회`() {
            // Given
            val saved = save(
                createMember(
                    email = "display@example.com",
                    username = "origin"
                )
            )

            // When - display name 변경 (username 변경)
            val found = memberRepository.findById(saved.id).orElseThrow()
            found.updateDisplayName("updated-name")
            memberRepository.save(found)
            entityManager.flush()
            entityManager.clear()

            // Then
            val updated = memberRepository.findByEmail("display@example.com")

            assertNotNull(updated)
            assertEquals("updated-name", updated!!.username)
        }


        @Test
        fun `프로필 업데이트 후 조회`() {
            val saved = save(createMember(email = "profile@example.com"))

            val found = memberRepository.findById(saved.id).orElseThrow()
            found.updateProfile(
                currentPositionId = 10L,
                careerYears = 5,
                summary = "시니어 개발자"
            )
            memberRepository.save(found)
            entityManager.flush()
            entityManager.clear()

            val updated = memberRepository.findByEmail("profile@example.com")

            assertNotNull(updated)
            assertEquals(10L, updated!!.currentPositionId)
            assertEquals(5, updated.careerYears)
            assertEquals("시니어 개발자", updated.summary)
        }

        @Test
        fun `OAuth 계정 연결 여부 확인`() {
            save(
                createMember(
                    email = "oauth@example.com",
                    provider = OAuth2Provider.GOOGLE,
                    providerUserId = "google-oauth-123"
                )
            )

            val found = memberRepository.findByEmail("oauth@example.com")

            assertNotNull(found)
            assertTrue(found!!.hasOAuthAccount(OAuth2Provider.GOOGLE))
        }
    }
}
