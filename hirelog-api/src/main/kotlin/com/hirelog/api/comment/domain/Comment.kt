package com.hirelog.api.comment.domain

import com.hirelog.api.common.infra.jpa.entity.BaseEntity
import jakarta.persistence.*

@Entity
@Table(
    name = "comment",
    indexes = [
        Index(name = "idx_comment_board", columnList = "board_id"),
        Index(name = "idx_comment_member", columnList = "member_id")
    ]
)
class Comment protected constructor(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,

    @Column(name = "board_id", nullable = false, updatable = false)
    val boardId: Long,

    @Column(name = "member_id", updatable = false)
    val memberId: Long?,

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    var content: String,

    @Column(name = "anonymous", nullable = false)
    var anonymous: Boolean,

    @Column(name = "deleted", nullable = false)
    var deleted: Boolean = false

) : BaseEntity() {

    fun update(content: String, anonymous: Boolean) {
        require(content.isNotBlank()) { "내용은 비어있을 수 없습니다" }
        this.content = content
        this.anonymous = anonymous
    }

    fun softDelete() {
        require(!deleted) { "이미 삭제된 댓글입니다" }
        this.deleted = true
    }

    fun isWrittenBy(memberId: Long) = this.memberId != null && this.memberId == memberId

    companion object {
        fun create(
            boardId: Long,
            memberId: Long?,
            content: String,
            anonymous: Boolean
        ): Comment {
            require(content.isNotBlank()) { "내용은 비어있을 수 없습니다" }
            require(content.length <= 1000) { "댓글은 1000자를 초과할 수 없습니다" }
            return Comment(
                boardId = boardId,
                memberId = memberId,
                content = content,
                anonymous = anonymous
            )
        }
    }
}
