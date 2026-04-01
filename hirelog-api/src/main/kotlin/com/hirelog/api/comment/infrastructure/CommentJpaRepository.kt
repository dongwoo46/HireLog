package com.hirelog.api.comment.infrastructure

import com.hirelog.api.comment.domain.Comment
import org.springframework.data.jpa.repository.JpaRepository

interface CommentJpaRepository : JpaRepository<Comment, Long>