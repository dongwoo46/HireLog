package com.hirelog.api.job.application.messaging

import com.hirelog.api.job.application.summary.command.JobSummaryGenerateCommand
import com.hirelog.api.job.domain.type.RecruitmentPeriodTypeMapper
import org.springframework.stereotype.Component

/**
 * JdPreprocessResponseEventMapper
 *
 * 책임:
 * - Kafka Event → Application Command 변환
 *
 * 원칙:
 * - Kafka 의존 ❌
 * - 비즈니스 로직 ❌
 * - 단순 형태 변환만 수행
 */
@Component
class JdPreprocessResponseEventMapper {

    fun toSummaryCommand(
        event: JdPreprocessResponseEvent
    ): JobSummaryGenerateCommand {

        return JobSummaryGenerateCommand(
            requestId = event.requestId,
            brandName = event.brandName,
            positionName = event.positionName,
            source = event.source,
            sourceUrl = event.sourceUrl,
            canonicalMap = event.canonicalMap,
            recruitmentPeriodType = RecruitmentPeriodTypeMapper.fromRaw(event.recruitmentPeriodType),
            openedDate = event.openedDate,
            closedDate = event.closedDate,
            skills = event.skills,
            occurredAt = event.occurredAt
        )
    }
}
