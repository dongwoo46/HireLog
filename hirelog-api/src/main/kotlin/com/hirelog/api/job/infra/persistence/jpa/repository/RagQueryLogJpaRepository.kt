package com.hirelog.api.job.infra.persistence.jpa.repository

import com.hirelog.api.job.domain.model.RagQueryLog
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor

interface RagQueryLogJpaRepository : JpaRepository<RagQueryLog, Long>, JpaSpecificationExecutor<RagQueryLog>