package com.hirelog.api.job.application.jdsummaryprocessing.port

import com.hirelog.api.job.domain.JdSummaryProcessing
import java.util.UUID

/**
 * JdSummaryProcessing Query Port
 *
 * 역할:
 * - Processing 상태 조회 책임 추상화
 * - 조회 로직을 Application에서 분리
 */
interface JdSummaryProcessingQuery {

    /**
     * ID 기준 단건 조회
     */
    fun findById(id: UUID): JdSummaryProcessing?

}
