package com.hirelog.api.relation.infra.persistence.jpa.adapter

import com.hirelog.api.relation.application.brand.query.MemberBrandQuery
import com.hirelog.api.relation.infra.persistence.jpa.repository.MemberBrandJpaRepository
import com.hirelog.api.relation.domain.model.MemberBrand
import org.springframework.stereotype.Component

/**
 * MemberBrand JPA Query Adapter
 *
 * 책임:
 * - MemberBrand 조회
 * - JPA Repository 위임
 */
@Component
class MemberBrandJpaQuery(
    private val repository: MemberBrandJpaRepository
) : MemberBrandQuery {

    override fun findAllByMemberId(memberId: Long): List<MemberBrand> {
        return repository.findAllByMemberId(memberId)
    }

    override fun findAllByBrandId(brandId: Long): List<MemberBrand> {
        return repository.findAllByBrandId(brandId)
    }

    override fun findByMemberIdAndBrandId(
        memberId: Long,
        brandId: Long
    ): MemberBrand? {
        return repository.findByMemberIdAndBrandId(memberId, brandId)
    }
}

