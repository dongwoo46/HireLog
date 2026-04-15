package com.hirelog.api.job.application.rag

import com.hirelog.api.common.application.port.PagedResult
import com.hirelog.api.job.application.rag.model.RagIntent
import com.hirelog.api.job.application.rag.port.RagQueryLogQuery
import com.hirelog.api.job.application.rag.view.RagQueryLogView
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
@Transactional(readOnly = true)
class RagLogReadService(
    private val ragQueryLogQuery: RagQueryLogQuery
) {

    /** 관리자: 전체 로그 조회 (memberId 필터 선택) */
    fun searchAdmin(
        memberId: Long?,
        intent: RagIntent?,
        dateFrom: LocalDate?,
        dateTo: LocalDate?,
        page: Int,
        size: Int
    ): PagedResult<RagQueryLogView> =
        ragQueryLogQuery.search(memberId, intent, dateFrom, dateTo, page, size)

    /** 일반 사용자: 자신의 로그만 조회 */
    fun searchMine(
        memberId: Long,
        intent: RagIntent?,
        dateFrom: LocalDate?,
        dateTo: LocalDate?,
        page: Int,
        size: Int
    ): PagedResult<RagQueryLogView> =
        ragQueryLogQuery.search(memberId, intent, dateFrom, dateTo, page, size)

    fun findById(id: Long): RagQueryLogView? =
        ragQueryLogQuery.findById(id)
}