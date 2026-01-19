package com.hirelog.api.relation.infra.persistence.jpa.adapter


import com.hirelog.api.relation.application.brand.command.MemberBrandCommand
import com.hirelog.api.relation.domain.model.MemberBrand
import com.hirelog.api.relation.infra.persistence.jpa.repository.MemberBrandJpaRepository
import org.springframework.stereotype.Component

/**
 * MemberBrand JPA Command Adapter
 *
 * 책임:
 * - MemberBrand 저장
 * - JPA Repository 위임
 */
@Component
class MemberBrandJpaCommand(
    private val repository: MemberBrandJpaRepository
) : MemberBrandCommand {

    override fun save(memberBrand: MemberBrand): MemberBrand {
        return repository.save(memberBrand)
    }
}
