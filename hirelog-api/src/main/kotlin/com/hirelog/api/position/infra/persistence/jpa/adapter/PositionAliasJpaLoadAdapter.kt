package com.hirelog.api.position.infra.persistence.jpa.adapter

import com.hirelog.api.position.application.port.PositionAliasLoad
import com.hirelog.api.position.domain.PositionAlias
import com.hirelog.api.position.infra.persistence.jpa.repository.PositionAliasJpaRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

/**
 * PositionAlias JPA Load Adapter
 *
 * 책임:
 * - PositionAliasLoad Port 구현
 * - Write 유스케이스 판단용 단건 조회
 */
@Component
class PositionAliasJpaLoadAdapter(
    private val repository: PositionAliasJpaRepository
) : PositionAliasLoad {

    override fun loadById(aliasId: Long): PositionAlias? =
        repository.findByIdOrNull(aliasId)

    override fun loadByNormalizedAliasName(
        normalizedAliasName: String
    ): PositionAlias? =
        repository.findByNormalizedAliasName(normalizedAliasName)
}
