package com.hirelog.api.job.presentation.controller.dto.response

import com.hirelog.api.job.application.rag.model.RagAnswer
import com.hirelog.api.job.application.rag.model.RagEvidence
import com.hirelog.api.job.application.rag.model.RagEvidenceType
import com.hirelog.api.job.application.rag.model.RagIntent

data class RagAnswerRes(
    val answer: String,
    val intent: RagIntent,
    val reasoning: String?,
    val evidences: List<RagEvidenceRes>?,
    val sources: List<RagSourceRes>?
) {
    companion object {
        fun from(answer: RagAnswer) = RagAnswerRes(
            answer = answer.answer,
            intent = answer.intent,
            reasoning = answer.reasoning,
            evidences = answer.evidences?.map { RagEvidenceRes.from(it) },
            sources = answer.sources?.map { RagSourceRes(id = it.id, brandName = it.brandName, positionName = it.positionName) }
        )
    }
}

data class RagEvidenceRes(
    val type: RagEvidenceType,
    val summary: String,
    val detail: String?,
    val sourceId: Long?
) {
    companion object {
        fun from(e: RagEvidence) = RagEvidenceRes(
            type = e.type,
            summary = e.summary,
            detail = e.detail,
            sourceId = e.sourceId
        )
    }
}

data class RagSourceRes(
    val id: Long,
    val brandName: String,
    val positionName: String
)