package com.hirelog.api.userrequest.domain

import com.hirelog.api.common.infra.jpa.entity.BaseEntity
import com.hirelog.api.common.infra.jpa.entity.VersionedEntity
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "user_request",
    indexes = [
        Index(name = "idx_user_request_member", columnList = "member_id"),
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

    @Column(name = "member_id", nullable = false, updatable = false)
    val memberId: Long,

    /* =========================
     * Request
     * ========================= */

    @Column(name = "title", nullable = false, updatable = false, length = 200)
    val title: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "request_type", nullable = false, updatable = false)
    val requestType: UserRequestType,

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    val content: String,

    /* =========================
     * Status
     * ========================= */

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private var status: UserRequestStatus = UserRequestStatus.OPEN,

    @Column(name = "resolved_at")
    private var resolvedAt: LocalDateTime? = null,

    /* =========================
     * Comments (Aggregate)
     * ========================= */

    @OneToMany(
        mappedBy = "userRequest",
        fetch = FetchType.LAZY,
        cascade = [CascadeType.ALL],
        orphanRemoval = true
    )
    private val comments: MutableList<UserRequestComment> = mutableListOf()

) : VersionedEntity() {

    companion object {

        fun create(
            memberId: Long,
            title: String,
            requestType: UserRequestType,
            content: String
        ): UserRequest {
            require(title.isNotBlank()) { "title must not be blank" }
            require(title.length <= 200) { "title must be <= 200 characters" }
            require(content.isNotBlank()) { "content must not be blank" }

            return UserRequest(
                memberId = memberId,
                title = title.trim(),
                requestType = requestType,
                content = content
            )
        }
    }

    /* =========================
     * Query helpers
     * ========================= */

    fun status(): UserRequestStatus = status

    fun resolvedAt(): LocalDateTime? = resolvedAt

    fun getComments(): List<UserRequestComment> =
        comments.toList()

    /* =========================
     * Domain behavior
     * ========================= */

    fun updateStatus(newStatus: UserRequestStatus) {
        // 이미 종결된 요청은 어떤 상태로도 변경 불가
        require(
            status != UserRequestStatus.RESOLVED &&
                    status != UserRequestStatus.REJECTED
        ) {
            "Closed request cannot be updated (current=$status)"
        }

        // 상태 변경
        this.status = newStatus

        // 종결 상태로 이동한 경우에만 resolvedAt 기록
        if (
            newStatus == UserRequestStatus.RESOLVED ||
            newStatus == UserRequestStatus.REJECTED
        ) {
            this.resolvedAt = LocalDateTime.now()
        }
    }

    /**
     * 댓글 추가 (Aggregate 내부에서만 허용)
     */
    fun addComment(
        writerType: UserRequestCommentWriterType,
        writerId: Long,
        content: String
    ): UserRequestComment {
        require(status == UserRequestStatus.OPEN) {
            "Cannot add comment to closed request"
        }

        val comment = UserRequestComment.create(
            userRequest = this,
            writerType = writerType,
            writerId = writerId,
            content = content
        )

        this.comments.add(comment)
        return comment
    }
}

