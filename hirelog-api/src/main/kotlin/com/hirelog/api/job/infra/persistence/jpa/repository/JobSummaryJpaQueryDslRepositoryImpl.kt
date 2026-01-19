package com.hirelog.api.job.infra.persistence.jpa.repository

import com.hirelog.api.job.domain.JobSummary
import com.hirelog.api.job.domain.QJobSummary
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class JobSummaryJpaQueryDslRepositoryImpl(
    private val queryFactory: JPAQueryFactory
) : JobSummaryJpaQueryDslRepository {

    override fun search(
        brandId: Long?,
        positionId: Long?,
        keyword: String?,
        pageable: Pageable
    ): Page<JobSummary> {

        val q = QJobSummary.jobSummary
        val conditions = mutableListOf<BooleanExpression>()

        brandId?.let { conditions += q.brandId.eq(it) }
        positionId?.let { conditions += q.positionId.eq(it) }
        keyword?.let {
            conditions += q.summaryText.containsIgnoreCase(it)
                .or(q.brandName.containsIgnoreCase(it))
                .or(q.positionName.containsIgnoreCase(it))
        }

        val content = queryFactory
            .selectFrom(q)
            .where(*conditions.toTypedArray())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        val total = queryFactory
            .select(q.count())
            .from(q)
            .where(*conditions.toTypedArray())
            .fetchOne() ?: 0L

        return PageImpl(content, pageable, total)
    }
}
