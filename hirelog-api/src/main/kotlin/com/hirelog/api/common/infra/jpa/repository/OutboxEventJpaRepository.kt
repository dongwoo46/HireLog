package com.hirelog.api.common.infra.jpa.repository

import com.hirelog.api.common.domain.outbox.OutboxStatus
import com.hirelog.api.common.infra.jpa.entity.OutboxEventJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.*

interface OutboxEventJpaRepository :
    JpaRepository<OutboxEventJpaEntity, UUID> {

}
