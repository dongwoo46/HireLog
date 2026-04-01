package com.hirelog.api.comment.infrastructure.adapter

import com.hirelog.api.comment.application.port.CommentCommand
import com.hirelog.api.comment.domain.Comment
import com.hirelog.api.comment.infrastructure.CommentJpaRepository
import org.springframework.stereotype.Component

@Component
class CommentJpaCommandAdapter(
    private val repository: CommentJpaRepository
) : CommentCommand {

    override fun save(comment: Comment): Comment = repository.save(comment)

    override fun findById(id: Long): Comment? = repository.findById(id).orElse(null)
}