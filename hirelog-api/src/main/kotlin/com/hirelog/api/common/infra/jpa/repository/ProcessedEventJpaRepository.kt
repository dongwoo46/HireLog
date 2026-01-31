package com.hirelog.api.common.infra.jpa.repository

import com.hirelog.api.common.infra.jpa.entity.ProcessedEventJpaEntity
import com.hirelog.api.common.infra.jpa.entity.ProcessedEventJpaId
import org.springframework.data.jpa.repository.JpaRepository

/**
 * ProcessedEventJpaRepository
 *
 * 책임:
 * - processed_event 테이블 접근
 */
interface ProcessedEventJpaRepository :
    JpaRepository<ProcessedEventJpaEntity, ProcessedEventJpaId>
