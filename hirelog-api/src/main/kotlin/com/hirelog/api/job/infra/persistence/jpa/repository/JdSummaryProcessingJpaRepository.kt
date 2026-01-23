package com.hirelog.api.job.infra.persistence.jpa.repository

import com.hirelog.api.job.domain.JdSummaryProcessing
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

/**
 * JdSummaryProcessing JPA Repository
 *
 * 역할:
 * - Spring Data JPA 기본 CRUD 제공
 * - 비즈니스 로직 포함 ❌
 */
interface JdSummaryProcessingJpaRepository :
    JpaRepository<JdSummaryProcessing, UUID> {

    /**
     * canonicalHash 기준 존재 여부 확인
     */
}
