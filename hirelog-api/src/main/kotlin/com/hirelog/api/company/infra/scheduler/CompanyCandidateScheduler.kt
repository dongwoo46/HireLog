package com.hirelog.api.company.infra.scheduler

import com.hirelog.api.common.logging.log
import com.hirelog.api.company.application.CompanyCandidateProcessingService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class CompanyCandidateScheduler(
    private val processingService: CompanyCandidateProcessingService
) {

    companion object {
        private const val ONE_SECOND = 1000L
        private const val ONE_MINUTE = 60 * ONE_SECOND
        private const val ONE_HOUR = 60 * ONE_MINUTE
    }

    /**
     * 승인된 CompanyCandidate 처리 스케줄러
     * * fixedDelay: 이전 작업이 종료된 시점부터 설정된 시간이 지난 후 다시 실행
     */
    @Scheduled(fixedDelay = ONE_HOUR * 10) // 10초 간격 설정 예시
    fun run() {
        log.debug("[CompanyCandidateScheduler] start")

        val candidates = processingService.fetchAndMarkProcessing(limit = 20)

        if (candidates.isEmpty()) {
            log.debug("[CompanyCandidateScheduler] no candidates")
            return
        }

        candidates.forEach { candidate ->
            try {
                processingService.process(candidate)
            } catch (ex: Exception) {
                log.error(
                    "[CompanyCandidateScheduler] processing failed. candidateId=${candidate.id}",
                    ex
                )
            }
        }

        log.debug("[CompanyCandidateScheduler] end")
    }
}