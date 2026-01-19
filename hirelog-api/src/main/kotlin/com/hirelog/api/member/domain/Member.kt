package com.hirelog.api.member.domain

import com.hirelog.api.common.jpa.BaseEntity
import jakarta.persistence.*

@Entity
@Table(
    name = "member",
    indexes = [
        Index(
            name = "idx_member_email",
            columnList = "email",
            unique = true
        )
    ]
)
class Member(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    /**
     * 로그인 식별자
     */
    @Column(name = "email", nullable = false, length = 255)
    val email: String,

    /**
     * 표시용 이름
     */
    @Column(name = "display_name", nullable = false, length = 100)
    val displayName: String,

    /**
     * 계정 상태
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    val status: MemberStatus = MemberStatus.ACTIVE,

    ) : BaseEntity()
