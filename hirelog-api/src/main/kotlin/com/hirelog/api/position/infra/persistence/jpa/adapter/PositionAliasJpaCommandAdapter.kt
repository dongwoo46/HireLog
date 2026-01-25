package com.hirelog.api.position.infra.persistence.jpa.adapter

import com.hirelog.api.position.application.port.PositionAliasCommand
import com.hirelog.api.position.domain.PositionAlias
import com.hirelog.api.position.infra.persistence.jpa.repository.PositionAliasJpaRepository
import org.springframework.stereotype.Component

/**
 * PositionAlias JPA Command Adapter
 *
 * 책임:
 * - PositionAliasCommand Port 구현
 * - 영속화만 담당
 */
@Component
class PositionAliasJpaCommandAdapter(
    private val repository: PositionAliasJpaRepository
) : PositionAliasCommand {

    override fun save(alias: PositionAlias): PositionAlias =
        repository.save(alias)
}
