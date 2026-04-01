package com.hirelog.api.comment.infrastructure

import com.hirelog.api.relation.domain.model.CommentLike
import org.springframework.data.jpa.repository.JpaRepository

interface CommentLikeJpaRepository : JpaRepository<CommentLike, Long> {
    fun findByMemberIdAndCommentId(memberId: Long, commentId: Long): CommentLike?
    fun countByCommentId(commentId: Long): Long
    fun existsByMemberIdAndCommentId(memberId: Long, commentId: Long): Boolean
}