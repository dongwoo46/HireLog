package com.hirelog.api.job.application.jdsummaryprocessing.port

import com.hirelog.api.job.domain.model.JdSummaryProcessing

/**
 * JdSummaryProcessing Command Port
 *
 * 역할:
 * - JdSummaryProcessing 영속화 책임 추상화
 * - Application 계층이 JPA 구현에 의존하지 않도록 보호
 *
 * 원칙:
 * - 상태 변경은 Domain(Entity)에서 수행
 * - Command는 변경 결과를 저장만 담당
 */
interface JdSummaryProcessingCommand {

    /**
     * 신규 Processing 저장
     */
    fun save(processing: JdSummaryProcessing)

    /**
     * 상태 변경 후 Processing 갱신
     */
    fun update(processing: JdSummaryProcessing)
}
