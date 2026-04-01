package com.hirelog.api.relation.domain.model

import com.hirelog.api.common.infra.jpa.entity.BaseEntity
import jakarta.persistence.*

@Entity
@Table(
    name = "comment_like",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_comment_like_member_comment",
            columnNames = ["member_id", "comment_id"]
        )
    ],
    indexes = [
        Index(name = "idx_comment_like_comment", columnList = "comment_id")
    ]
)
class CommentLike protected constructor(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,

    @Column(name = "member_id", nullable = false, updatable = false)
    val memberId: Long,

    @Column(name = "comment_id", nullable = false, updatable = false)
    val commentId: Long

) : BaseEntity() {

    companion object {
        fun create(memberId: Long, commentId: Long) = CommentLike(
            memberId = memberId,
            commentId = commentId
        )
    }
}