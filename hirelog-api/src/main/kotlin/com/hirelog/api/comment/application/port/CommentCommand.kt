package com.hirelog.api.comment.application.port

import com.hirelog.api.comment.domain.Comment

interface CommentCommand {
    fun save(comment: Comment): Comment
    fun findById(id: Long): Comment?
}