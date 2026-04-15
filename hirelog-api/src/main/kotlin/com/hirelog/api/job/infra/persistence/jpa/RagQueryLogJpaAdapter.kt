package com.hirelog.api.job.infra.persistence.jpa

import com.hirelog.api.job.application.rag.port.RagQueryLogCommand
import com.hirelog.api.job.domain.model.RagQueryLog
import com.hirelog.api.job.infra.persistence.jpa.repository.RagQueryLogJpaRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class RagQueryLogJpaAdapter(
    private val repository: RagQueryLogJpaRepository
) : RagQueryLogCommand {

    @Transactional
    override fun save(log: RagQueryLog) {
        repository.save(log)
    }
}