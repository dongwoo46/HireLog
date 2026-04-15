package com.hirelog.api.job.application.rag.port

import com.hirelog.api.common.application.port.PagedResult
import com.hirelog.api.job.application.rag.model.RagIntent
import com.hirelog.api.job.application.rag.view.RagQueryLogView
import java.time.LocalDate

interface RagQueryLogQuery {

    /**
     * RAG 질의 로그 페이징 조회
     *
     * @param memberId  null → 전체 조회 (admin), non-null → 특정 멤버만
     * @param intent    null → 전체 intent
     * @param dateFrom  null → 시작일 제한 없음 (inclusive)
     * @param dateTo    null → 종료일 제한 없음 (inclusive)
     */
    fun search(
        memberId: Long?,
        intent: RagIntent?,
        dateFrom: LocalDate?,
        dateTo: LocalDate?,
        page: Int,
        size: Int
    ): PagedResult<RagQueryLogView>

    fun findById(id: Long): RagQueryLogView?
}
