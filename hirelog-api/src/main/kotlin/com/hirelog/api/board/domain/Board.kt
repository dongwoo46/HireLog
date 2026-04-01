package com.hirelog.api.board.domain

import com.hirelog.api.common.infra.jpa.entity.BaseEntity
import jakarta.persistence.*

@Entity
@Table(
    name = "board",
    indexes = [
        Index(name = "idx_board_member", columnList = "member_id"),
        Index(name = "idx_board_deleted_created", columnList = "deleted, created_at")
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
    var deleted: Boolean = false

) : BaseEntity() {

    fun update(title: String, content: String, anonymous: Boolean) {
        require(title.isNotBlank()) { "제목은 비어있을 수 없습니다" }
        require(content.isNotBlank()) { "내용은 비어있을 수 없습니다" }
        this.title = title
        this.content = content
        this.anonymous = anonymous
    }

    fun softDelete() {
        require(!deleted) { "이미 삭제된 게시글입니다" }
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
            guestPasswordHash: String?
        ): Board {
            require(title.isNotBlank()) { "제목은 비어있을 수 없습니다" }
            require(title.length <= 300) { "제목은 300자를 초과할 수 없습니다" }
            require(content.isNotBlank()) { "내용은 비어있을 수 없습니다" }
            if (memberId == null) {
                require(!guestPasswordHash.isNullOrBlank()) { "비로그인 작성은 비밀번호가 필요합니다." }
            }
            return Board(
                memberId = memberId,
                boardType = boardType,
                title = title,
                content = content,
                anonymous = anonymous,
                guestPasswordHash = guestPasswordHash
            )
        }
    }
}
