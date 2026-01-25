package com.hirelog.api.job.application.summary.port

import com.hirelog.api.job.domain.JobSummary

/**
 * JobSummary Command Port
 *
 * 책임:
 * - 완성된 JobSummary 영속화
 * - 저장소 / 색인 전략을 알지 않는다
 *
 * 설계 원칙:
 * - 생성 로직 ❌
 * - 정책 판단 ❌
 * - 영속화만 담당
 */
interface JobSummaryCommand {

    /**
     * JobSummary 저장
     *
     * @return 저장된 JobSummary
     */
    fun save(summary: JobSummary): JobSummary
}
