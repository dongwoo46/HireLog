package com.hirelog.api.userrequest.domain

import com.hirelog.api.common.infra.jpa.entity.BaseEntity
import jakarta.persistence.*

@Entity
@Table(
    name = "user_request_comment",
    indexes = [
        Index(name = "idx_user_request_comment_request", columnList = "user_request_id"),
        Index(name = "idx_user_request_comment_writer", columnList = "writer_type")
    ]
)
class UserRequestComment(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    /* =========================
     * Parent
     * ========================= */

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_request_id", nullable = false)
    val userRequest: UserRequest,

    /* =========================
     * Writer
     * ========================= */

    @Enumerated(EnumType.STRING)
    @Column(name = "writer_type", nullable = false)
    val writerType: UserRequestCommentWriterType,

    @Column(name = "writer_id", nullable = false)
    val writerId: Long,

    /* =========================
     * Content
     * ========================= */

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    val content: String
) : BaseEntity()
