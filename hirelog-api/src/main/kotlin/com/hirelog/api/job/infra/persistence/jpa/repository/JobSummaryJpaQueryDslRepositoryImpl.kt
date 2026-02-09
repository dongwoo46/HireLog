package com.hirelog.api.job.infra.persistence.jpa.repository

import com.hirelog.api.job.domain.model.QJobSummary
import com.hirelog.api.job.infra.persistence.jpa.projection.JobSummaryProjection
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.core.types.Projections
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class JobSummaryJpaQueryDslRepositoryImpl(
    private val queryFactory: JPAQueryFactory
) : JobSummaryJpaQueryDslRepository {

    /**
     * JobSummary 조회 (Projection 기반)
     *
     * 설계 포인트:
     * - Entity(JobSummary)를 절대 생성하지 않는다
     * - 영속성 컨텍스트 / Lazy 로딩 / Dirty Checking 없음
     * - select 결과를 바로 Projection으로 받는다
     */
    override fun search(
        brandId: Long?,
        positionId: Long?,
        keyword: String?,
        pageable: Pageable
    ): Page<JobSummaryProjection> {

        val q = QJobSummary.jobSummary
        val conditions = mutableListOf<BooleanExpression>()

        // 0️⃣ 활성화된 것만 조회 (필수)
        conditions += q.isActive.isTrue

        // 1️⃣ 검색 조건 구성
        brandId?.let { conditions += q.brandId.eq(it) }
        positionId?.let { conditions += q.positionId.eq(it) }
        keyword?.let {
            conditions += q.summaryText.containsIgnoreCase(it)
                .or(q.brandName.containsIgnoreCase(it))
                .or(q.positionName.containsIgnoreCase(it))
        }

        // 2️⃣ 조회 쿼리 (Projection)
        // - selectFrom ❌
        // - Projections.fields ⭕
        val content = queryFactory
            .select(
                Projections.fields(
                    JobSummaryProjection::class.java,

                    // --- 식별자 ---
                    q.id.`as`("summaryId"),
                    q.jobSnapshotId.`as`("snapshotId"),

                    // --- 브랜드 ---
                    q.brandId,
                    q.brandName,

                    // --- 포지션 ---
                    q.positionId,
                    q.positionName,

                    // --- 브랜드 포지션 / 카테고리 ---
                    q.brandPositionId,
                    q.positionCategoryId,
                    q.positionCategoryName,

                    // --- 커리어 ---
                    q.careerType,
                    q.careerYears,

                    // --- 요약 결과 ---
                    q.summaryText.`as`("summary"),
                    q.responsibilities,
                    q.requiredQualifications,
                    q.preferredQualifications,
                    q.techStack
                )
            )
            .from(q)
            .where(*conditions.toTypedArray())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        // ----------------------------------------
        // 3️⃣ 전체 개수 조회 (count)
        // - Projection과 무관
        // ----------------------------------------
        val total = queryFactory
            .select(q.count())
            .from(q)
            .where(*conditions.toTypedArray())
            .fetchOne() ?: 0L

        return PageImpl(content, pageable, total)
    }
}
