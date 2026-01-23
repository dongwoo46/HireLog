package com.hirelog.api.job.infra.persistence.jpa.adapter

import com.hirelog.api.job.application.jdsummaryprocessing.port.JdSummaryProcessingCommand
import com.hirelog.api.job.domain.JdSummaryProcessing
import com.hirelog.api.job.infra.persistence.jpa.repository.JdSummaryProcessingJpaRepository
import org.springframework.stereotype.Component

/**
 * JdSummaryProcessing JPA Command Adapter
 *
 * 역할:
 * - Command Port의 JPA 구현체
 * - Application 계층과 JPA 사이의 경계 역할
 *
 * 주의:
 * - 상태 변경 로직 ❌
 * - 단순 영속화 책임만 수행
 */
@Component
class JdSummaryProcessingJpaCommandAdapter(
    private val repository: JdSummaryProcessingJpaRepository
) : JdSummaryProcessingCommand {

    override fun save(processing: JdSummaryProcessing) {
        repository.save(processing)
    }

    override fun update(processing: JdSummaryProcessing) {
        repository.save(processing)
    }
}
