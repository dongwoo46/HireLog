package com.hirelog.api.job.application.rag.view

import com.hirelog.api.job.application.rag.model.RagIntent
import com.hirelog.api.job.domain.model.RagQueryLog
import java.time.LocalDateTime

data class RagQueryLogView(
    val id: Long,
    val memberId: Long,
    val question: String,
    val intent: RagIntent,
    val parsedText: String?,
    val parsedFiltersJson: String?,
    val contextJson: String?,
    val answer: String,
    val reasoning: String?,
    val evidencesJson: String?,
    val sourcesJson: String?,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(entity: RagQueryLog) = RagQueryLogView(
            id = entity.id,
            memberId = entity.memberId,
            question = entity.question,
            intent = entity.intent,
            parsedText = entity.parsedText,
            parsedFiltersJson = entity.parsedFiltersJson,
            contextJson = entity.contextJson,
            answer = entity.answer,
            reasoning = entity.reasoning,
            evidencesJson = entity.evidencesJson,
            sourcesJson = entity.sourcesJson,
            createdAt = entity.createdAt
        )
    }
}
