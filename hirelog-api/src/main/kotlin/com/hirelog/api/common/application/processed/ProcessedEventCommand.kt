package com.hirelog.api.common.application.processed

import com.hirelog.api.common.domain.process.ProcessedEvent

/**
 * ProcessedEventCommand
 *
 * 책임:
 * - 처리 완료 이벤트를 영속화
 *
 * 규칙:
 * - 중복 저장 여부 판단은 DB 제약에 위임
 */
interface ProcessedEventCommand {

    /**
     * 처리 완료 이벤트 저장
     *
     * 중복 이벤트인 경우 DB 제약에 의해 예외가 발생할 수 있다.
     */
    fun save(processedEvent: ProcessedEvent)
}
