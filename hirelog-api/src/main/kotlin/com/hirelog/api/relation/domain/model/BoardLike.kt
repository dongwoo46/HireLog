package com.hirelog.api.relation.domain.model

import com.hirelog.api.common.infra.jpa.entity.BaseEntity
import jakarta.persistence.*

@Entity
@Table(
    name = "board_like",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_board_like_member_board",
            columnNames = ["member_id", "board_id"]
        )
    ],
    indexes = [
        Index(name = "idx_board_like_board", columnList = "board_id")
    ]
)
class BoardLike protected constructor(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,

    @Column(name = "member_id", nullable = false, updatable = false)
    val memberId: Long,

    @Column(name = "board_id", nullable = false, updatable = false)
    val boardId: Long

) : BaseEntity() {

    companion object {
        fun create(memberId: Long, boardId: Long) = BoardLike(
            memberId = memberId,
            boardId = boardId
        )
    }
}
