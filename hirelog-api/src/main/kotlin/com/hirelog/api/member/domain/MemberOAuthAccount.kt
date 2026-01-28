package com.hirelog.api.member.domain

import com.hirelog.api.auth.domain.OAuth2Provider
import com.hirelog.api.common.jpa.BaseEntity
import jakarta.persistence.*

@Entity
@Table(
    name = "member_oauth_account",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_provider_user",
            columnNames = ["provider", "provider_user_id"]
        )
    ]
)
class MemberOAuthAccount(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    val member: Member,

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 20)
    val provider: OAuth2Provider,

    @Column(name = "provider_user_id", nullable = false, length = 255)
    val providerUserId: String

) : BaseEntity() {
    companion object {

        /**
         * OAuth 계정 연결 생성
         *
         * 책임:
         * - 필수 값 검증
         * - 생성 규칙 캡슐화
         */
        fun create(
            member: Member,
            provider: OAuth2Provider,
            providerUserId: String
        ): MemberOAuthAccount {
            require(providerUserId.isNotBlank()) {
                "providerUserId는 비어 있을 수 없습니다."
            }

            return MemberOAuthAccount(
                member = member,
                provider = provider,
                providerUserId = providerUserId
            )
        }
    }
}
