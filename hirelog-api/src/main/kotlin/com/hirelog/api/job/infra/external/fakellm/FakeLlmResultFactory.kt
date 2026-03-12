package com.hirelog.api.job.infra.external.fakellm

import com.hirelog.api.common.domain.LlmProvider
import com.hirelog.api.job.application.summary.view.JobSummaryInsightResult
import com.hirelog.api.job.application.summary.view.JobSummaryLlmResult
import com.hirelog.api.job.domain.type.CareerType
import com.hirelog.api.position.infra.persistence.jpa.repository.PositionJpaRepository
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Profile("loadtest")
class FakeLlmResultFactory {

    private val positionPool = listOf(
        "Backend Engineer",
        "Frontend Engineer",
        "Fullstack Engineer",
        "Mobile App Engineer",
        "System Engineer",
        "Embedded Engineer"
    )

    fun generate(
        seq: Long,
        provider: LlmProvider
    ): JobSummaryLlmResult {

        val resolvedPosition =
            positionPool[(seq % positionPool.size).toInt()]

        val careerType =
            if (seq % 6 == 0L)
                CareerType.NEW
            else
                CareerType.EXPERIENCED

        val careerYears =
            if (careerType == CareerType.NEW)
                null
            else
                "${(seq % 12) + 1}년 이상"

        val insight = JobSummaryInsightResult(
            idealCandidate = "Self-driven engineer level ${(seq % 5) + 1}",
            mustHaveSignals = "Handled ${(1000 + seq % 1000)} TPS",
            preparationFocus = "System design & concurrency",
            transferableStrengthsAndGapPlan = "Monolith → MSA migration ${seq % 20}",
            proofPointsAndMetrics = "Latency ${(10 + seq % 200)} ms",
            storyAngles = "Optimization case ${seq % 15}",
            keyChallenges =
            if (seq % 4 == 0L) "Legacy migration"
            else "Traffic spike handling",
            technicalContext =
            if (seq % 2 == 0L) "Kafka-based architecture"
            else "REST microservices",
            questionsToAsk = "Peak traffic handling strategy?",
            considerations =
            if (seq % 9 == 0L) null
            else "On-call rotation"
        )

        return JobSummaryLlmResult(
            llmProvider = provider,
            brandName = "Brand-${seq % 200}",
            positionName = resolvedPosition,
            companyCandidate = "Company-${seq % 500}",
            careerType = careerType,
            careerYears = careerYears,
            summary = "Role: $resolvedPosition Batch-$seq",
            responsibilities = "API design (${seq % 50})",
            requiredQualifications = "Spring ${1 + (seq % 7)}y+",
            preferredQualifications =
            if (seq % 3 == 0L) "Kafka, Redis"
            else "Cloud infra",
            techStack = "Spring, Kafka",
            recruitmentProcess = "서류 → 면접",
            insight = insight
        )
    }
}
