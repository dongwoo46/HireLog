package com.hirelog.api.job.infra.persistence.jpa

import com.hirelog.api.common.application.port.PagedResult
import com.hirelog.api.job.application.rag.model.RagIntent
import com.hirelog.api.job.application.rag.port.RagQueryLogQuery
import com.hirelog.api.job.application.rag.view.RagQueryLogView
import com.hirelog.api.job.domain.model.RagQueryLog
import com.hirelog.api.job.infra.persistence.jpa.repository.RagQueryLogJpaRepository
import jakarta.persistence.criteria.Predicate
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Component
class RagQueryLogJpaQueryAdapter(
    private val repository: RagQueryLogJpaRepository
) : RagQueryLogQuery {

    @Transactional(readOnly = true)
    override fun search(
        memberId: Long?,
        intent: RagIntent?,
        dateFrom: LocalDate?,
        dateTo: LocalDate?,
        page: Int,
        size: Int
    ): PagedResult<RagQueryLogView> {
        require(page >= 0) { "page must be >= 0" }
        require(size in 1..100) { "size must be between 1 and 100" }

        val spec = buildSpec(memberId, intent, dateFrom, dateTo)
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        val result = repository.findAll(spec, pageable)

        return PagedResult.of(
            items = result.content.map { RagQueryLogView.from(it) },
            page = page,
            size = size,
            totalElements = result.totalElements
        )
    }

    @Transactional(readOnly = true)
    override fun findById(id: Long): RagQueryLogView? =
        repository.findById(id).orElse(null)?.let { RagQueryLogView.from(it) }

    private fun buildSpec(
        memberId: Long?,
        intent: RagIntent?,
        dateFrom: LocalDate?,
        dateTo: LocalDate?
    ): Specification<RagQueryLog> = Specification { root, _, cb ->
        val predicates = mutableListOf<Predicate>()

        memberId?.let { predicates.add(cb.equal(root.get<Long>("memberId"), it)) }
        intent?.let { predicates.add(cb.equal(root.get<RagIntent>("intent"), it)) }
        dateFrom?.let { predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), it.atStartOfDay())) }
        dateTo?.let { predicates.add(cb.lessThan(root.get("createdAt"), it.plusDays(1).atStartOfDay())) }

        cb.and(*predicates.toTypedArray())
    }
}