package com.hirelog.api.userrequest.domain

import com.hirelog.api.common.infra.jpa.entity.BaseEntity
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "user_request",
    indexes = [
        Index(name = "idx_member_request_user", columnList = "member_id"),
        Index(name = "idx_user_request_status", columnList = "status"),
    ]
)
class UserRequest(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    /* =========================
     * Requester
     * ========================= */

    @Column(name = "member_id", nullable = false)
    val memberId: Long,

    /* =========================
     * Request
     * ========================= */

    @Enumerated(EnumType.STRING)
    @Column(name = "request_type", nullable = false)
    val requestType: UserRequestType,

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    val content: String,

    /* =========================
     * Status
     * ========================= */

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: UserRequestStatus = UserRequestStatus.OPEN,

    @Column(name = "resolved_at")
    var resolvedAt: LocalDateTime? = null
) : BaseEntity()
