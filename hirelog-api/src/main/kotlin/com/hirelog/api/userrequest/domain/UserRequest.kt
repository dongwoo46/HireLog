package com.hirelog.api.userrequest.domain

import com.hirelog.api.common.infra.jpa.entity.BaseEntity
import com.hirelog.api.common.infra.jpa.entity.VersionedEntity
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
class UserRequest protected constructor(

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
) : VersionedEntity() {

    companion object {
        fun create(
            memberId: Long,
            requestType: UserRequestType,
            content: String
        ): UserRequest = UserRequest(
            memberId = memberId,
            requestType = requestType,
            content = content
        )
    }

    /**
     * 상태 변경
     *
     * 정책:
     * - RESOLVED / REJECTED 상태로 전이 시 resolvedAt 자동 설정
     */
    fun updateStatus(newStatus: UserRequestStatus) {
        this.status = newStatus

        if (newStatus == UserRequestStatus.RESOLVED || newStatus == UserRequestStatus.REJECTED) {
            this.resolvedAt = LocalDateTime.now()
        }
    }
}
