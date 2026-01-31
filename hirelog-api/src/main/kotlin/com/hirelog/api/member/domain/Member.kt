package com.hirelog.api.member.domain

import com.hirelog.api.auth.domain.OAuth2Provider
import com.hirelog.api.common.infra.jpa.entity.BaseEntity
import jakarta.persistence.*

/**
 * Member 도메인 엔티티 (Aggregate Root)
 *
 * 책임:
 * - 서비스 상의 "사람"을 표현
 * - 계정 상태 및 권한 관리
 * - OAuth 계정 연결의 생명주기 소유
 *
 * 설계 원칙:
 * - Member는 자신의 상태를 스스로 변경한다
 * - 의미 없는 상태 변경은 메서드로 차단한다
 */
@Entity
@Table(
    name = "member",
    indexes = [
        Index(
            name = "idx_member_email",
            columnList = "email",
            unique = true
        ),
        Index(
            name = "ux_member_username",
            columnList = "username",
            unique = true
        )
    ]
)
class Member protected constructor(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,

    @Column(name = "email", nullable = false, length = 255)
    val email: String,

    @Column(name = "username", nullable = false, length = 100)
    var username: String,

    @Column(name = "current_position_id")
    var currentPositionId: Long? = null,

    @Column(name = "career_years")
    var careerYears: Int? = null,

    @Column(name = "summary", length = 1000)
    var summary: String? = null,

) : BaseEntity() {

    /**
     * 회원 권한
     *
     * - protected set → 도메인 메서드로만 변경 가능
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    var role: MemberRole = MemberRole.USER
        protected set

    /**
     * 계정 상태
     *
     * - ACTIVE / SUSPENDED / DELETED
     * - protected set → 도메인 메서드로만 변경 가능
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: MemberStatus = MemberStatus.ACTIVE
        protected set

    /**
     * OAuth 계정 목록
     *
     * - Member Aggregate 내부 상태
     * - 생명주기(cascade/orphanRemoval)는 Member가 소유
     */
    @OneToMany(
        mappedBy = "member",
        cascade = [CascadeType.ALL],
        orphanRemoval = true
    )
    private val oauthAccounts: MutableList<MemberOAuthAccount> = mutableListOf()

    /* =========================
     * Factory
     * ========================= */

    companion object {

        fun createByOAuth(
            email: String,
            username: String,
            provider: OAuth2Provider,
            providerUserId: String,
            currentPositionId: Long? = null,
            careerYears: Int? = null,
            summary: String? = null,
        ): Member {
            require(email.isNotBlank())
            require(username.isNotBlank())
            require(careerYears == null || careerYears >= 0)
            require(summary == null || summary.length <= 1000)

            val member = Member(
                email = email,
                username = username,
                currentPositionId = currentPositionId,
                careerYears = careerYears,
                summary = summary,
            )
            member.linkOAuthAccount(provider, providerUserId)
            return member
        }
    }

    /* =========================
     * Profile
     * ========================= */

    fun updateDisplayName(newUsername: String) {
        require(newUsername.isNotBlank())
        username = newUsername
    }

    fun updateProfile(
        currentPositionId: Long?,
        careerYears: Int?,
        summary: String?,
    ) {
        require(careerYears == null || careerYears >= 0)
        require(summary == null || summary.length <= 1000)

        this.currentPositionId = currentPositionId
        this.careerYears = careerYears
        this.summary = summary
    }

    /* =========================
     * OAuth
     * ========================= */

    fun linkOAuthAccount(
        provider: OAuth2Provider,
        providerUserId: String,
    ) {
        require(status == MemberStatus.ACTIVE)
        require(oauthAccounts.none { it.provider == provider })

        oauthAccounts.add(
            MemberOAuthAccount.create(
                member = this,
                provider = provider,
                providerUserId = providerUserId,
            )
        )
    }

    fun hasOAuthAccount(provider: OAuth2Provider): Boolean =
        oauthAccounts.any { it.provider == provider }

    /* =========================
     * Lifecycle
     * ========================= */

    fun suspend() {
        require(status == MemberStatus.ACTIVE)
        status = MemberStatus.SUSPENDED
    }

    fun activate() {
        require(status == MemberStatus.SUSPENDED)
        status = MemberStatus.ACTIVE
    }

    fun softDelete() {
        require(status != MemberStatus.DELETED)
        status = MemberStatus.DELETED
    }
}
