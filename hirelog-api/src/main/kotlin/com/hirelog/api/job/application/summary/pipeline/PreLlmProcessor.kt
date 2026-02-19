package com.hirelog.api.job.application.summary.pipeline

import com.hirelog.api.common.logging.log
import com.hirelog.api.company.application.port.CompanyQuery
import com.hirelog.api.job.application.intake.JdIntakePolicy
import com.hirelog.api.job.application.intake.model.DuplicateDecision
import com.hirelog.api.job.application.jobsummaryprocessing.JdSummaryProcessingWriteService
import com.hirelog.api.job.application.snapshot.JobSnapshotWriteService
import com.hirelog.api.job.application.snapshot.command.JobSnapshotCreateCommand
import com.hirelog.api.job.application.summary.command.JobSummaryGenerateCommand
import com.hirelog.api.job.application.summary.port.JobSummaryQuery
import com.hirelog.api.job.domain.type.JobSourceType
import com.hirelog.api.position.application.port.PositionQuery
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class PreLlmProcessor(
    private val processingWriteService: JdSummaryProcessingWriteService,
    private val snapshotWriteService: JobSnapshotWriteService,
    private val jdIntakePolicy: JdIntakePolicy,
    private val summaryQuery: JobSummaryQuery,
    private val positionQuery: PositionQuery,
    private val companyQuery: CompanyQuery
) {

    data class PreLlmResult(
        val snapshotId: Long,
        val positionCandidates: List<String>,
        val existCompanies: List<String>
    )

    /**
     * Pre-LLM 단계 실행
     *
     * @return 성공 시 PreLlmResult, 중복/무효 시 null
     */
    fun execute(processingId: UUID, command: JobSummaryGenerateCommand): PreLlmResult? {

        if (!jdIntakePolicy.isValidJd(command)) {
            processingWriteService.markFailed(
                processingId = processingId,
                errorCode = "INVALID_INPUT",
                errorMessage = "JD 유효성 검증 실패"
            )
            return null
        }

        if (command.source == JobSourceType.URL && command.sourceUrl != null) {
            if (summaryQuery.existsBySourceUrl(command.sourceUrl)) {
                processingWriteService.markDuplicate(
                    processingId = processingId,
                    reason = "URL_DUPLICATE"
                )
                return null
            }
        }

        val hashes = jdIntakePolicy.generateIntakeHashes(command.canonicalMap)
        val decision = jdIntakePolicy.decideDuplicate(command, hashes)

        val snapshotId = when (decision) {
            is DuplicateDecision.Duplicate -> {
                processingWriteService.markDuplicate(
                    processingId = processingId,
                    reason = decision.reason.name
                )
                log.info(
                    "[JD_DUPLICATE_DETECTED] reason={}, existingSnapshotId={}, existingSummaryId={}",
                    decision.reason, decision.existingSnapshotId, decision.existingSummaryId
                )
                return null
            }

            is DuplicateDecision.Reprocessable -> {
                log.info("[JD_REPROCESS] existingSnapshotId={}", decision.existingSnapshotId)
                decision.existingSnapshotId
            }

            is DuplicateDecision.NotDuplicate -> {
                snapshotWriteService.record(
                    JobSnapshotCreateCommand(
                        sourceType = command.source,
                        sourceUrl = command.sourceUrl,
                        canonicalMap = command.canonicalMap,
                        coreText = hashes.coreText,
                        recruitmentPeriodType = command.recruitmentPeriodType,
                        openedDate = command.openedDate,
                        closedDate = command.closedDate,
                        canonicalHash = hashes.canonicalHash,
                        simHash = hashes.simHash
                    )
                )
            }
        }

        processingWriteService.markSummarizing(processingId, snapshotId)

        val positionCandidates = positionQuery.findActiveNames()
        val existCompanies = companyQuery.findAllNames().map { it.name }

        return PreLlmResult(
            snapshotId = snapshotId,
            positionCandidates = positionCandidates,
            existCompanies = existCompanies
        )
    }
}
