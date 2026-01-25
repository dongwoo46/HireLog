package com.hirelog.api.position.infra.persistence.jpa.adapter

import com.hirelog.api.position.application.port.PositionAliasQuery
import com.hirelog.api.position.application.query.PositionAliasView
import com.hirelog.api.position.domain.PositionAliasStatus
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Component

/**
 * PositionAlias JPA Query Adapter
 *
 * 책임:
 * - PositionAliasQuery Port 구현
 * - View 전용 조회
 */
@Component
class PositionAliasJpaQueryAdapter(
    private val em: EntityManager
) : PositionAliasQuery {

    override fun findActiveByNormalizedAlias(
        normalizedAliasName: String
    ): PositionAliasView? {

        val jpql = """
            select new com.hirelog.api.position.application.query.PositionAliasView(
                a.id,
                a.aliasName,
                a.normalizedAliasName,
                a.status,
                a.position.id
            )
            from PositionAlias a
            where a.normalizedAliasName = :normalizedAliasName
              and a.status = :status
        """.trimIndent()

        return em.createQuery(jpql, PositionAliasView::class.java)
            .setParameter("normalizedAliasName", normalizedAliasName)
            .setParameter("status", PositionAliasStatus.ACTIVE)
            .resultList
            .firstOrNull()
    }

    override fun listByPosition(
        positionId: Long
    ): List<PositionAliasView> {

        val jpql = """
            select new com.hirelog.api.position.application.query.PositionAliasView(
                a.id,
                a.aliasName,
                a.normalizedAliasName,
                a.status,
                a.position.id
            )
            from PositionAlias a
            where a.position.id = :positionId
            order by a.createdAt asc
        """.trimIndent()

        return em.createQuery(jpql, PositionAliasView::class.java)
            .setParameter("positionId", positionId)
            .resultList
    }
}
