package com.hirelog.api.board.domain

import com.hirelog.api.common.infra.jpa.entity.BaseEntity
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "board",
    indexes = [
        Index(name = "idx_board_member", columnList = "member_id"),
        Index(name = "idx_board_deleted_created", columnList = "deleted, created_at"),
        Index(name = "idx_board_pinned_created", columnList = "pinned, created_at")
    ]
)
class Board protected constructor(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,

    @Column(name = "member_id", updatable = false)
    val memberId: Long?,

    @Enumerated(EnumType.STRING)
    @Column(name = "board_type", nullable = false, length = 30, updatable = false)
    val boardType: BoardType,

    @Column(name = "title", nullable = false, length = 300)
    var title: String,

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    var content: String,

    @Column(name = "anonymous", nullable = false)
    var anonymous: Boolean,

    @Column(name = "guest_password_hash", length = 100)
    var guestPasswordHash: String? = null,

    @Column(name = "deleted", nullable = false)
    var deleted: Boolean = false,

    @Column(name = "notice", nullable = false)
    var notice: Boolean = false,

    @Column(name = "pinned", nullable = false)
    var pinned: Boolean = false,

    @Column(name = "pinned_at")
    var pinnedAt: LocalDateTime? = null

) : BaseEntity() {

    fun update(title: String, content: String, anonymous: Boolean, notice: Boolean, pinned: Boolean) {
        require(title.isNotBlank()) { "title must not be blank" }
        require(content.isNotBlank()) { "content must not be blank" }
        this.title = title
        this.content = content
        this.anonymous = anonymous
        this.notice = notice
        applyPinned(pinned)
    }

    fun applyPinned(pinned: Boolean) {
        if (this.pinned == pinned) return
        this.pinned = pinned
        this.pinnedAt = if (pinned) LocalDateTime.now() else null
    }

    fun softDelete() {
        require(!deleted) { "board already deleted" }
        this.deleted = true
    }

    fun isWrittenBy(memberId: Long) = this.memberId != null && this.memberId == memberId

    fun isGuestPost() = this.memberId == null

    companion object {
        fun create(
            memberId: Long?,
            boardType: BoardType,
            title: String,
            content: String,
            anonymous: Boolean,
            guestPasswordHash: String?,
            notice: Boolean,
            pinned: Boolean
        ): Board {
            require(title.isNotBlank()) { "title must not be blank" }
            require(title.length <= 300) { "title too long" }
            require(content.isNotBlank()) { "content must not be blank" }
            if (memberId == null) {
                require(!guestPasswordHash.isNullOrBlank()) { "guest password required" }
            }
            return Board(
                memberId = memberId,
                boardType = boardType,
                title = title,
                content = content,
                anonymous = anonymous,
                guestPasswordHash = guestPasswordHash,
                notice = notice,
                pinned = pinned,
                pinnedAt = if (pinned) LocalDateTime.now() else null
            )
        }
    }
}
