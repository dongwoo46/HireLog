package com.hirelog.api.job.application.jobsummaryprocessing

import com.hirelog.api.job.application.jdsummaryprocessing.port.JdSummaryProcessingCommand
import com.hirelog.api.job.application.jdsummaryprocessing.port.JdSummaryProcessingQuery
import com.hirelog.api.job.domain.model.JdSummaryProcessing
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class JdSummaryProcessingWriteService(
    private val command: JdSummaryProcessingCommand,
    private val query: JdSummaryProcessingQuery
) {

    /**
     * Processing 생성
     *
     * 규칙:
     * - requestId는 외부 요청과 1:1 매핑
     * - 최초 상태는 RECEIVED
     */
    @Transactional
    fun startProcessing(
        requestId: String,
    ): JdSummaryProcessing {

        val processing = JdSummaryProcessing.create(
            id = UUID.fromString(requestId),
        )

        command.save(processing)
        return processing
    }

    /**
     * LLM 요약 시작 상태로 전이 + Snapshot 연결
     */
    @Transactional
    fun markSummarizing(processingId: UUID, snapshotId: Long) {
        val processing = getRequired(processingId)
        processing.markSummarizing(snapshotId)
        command.update(processing)
    }

    /**
     * LLM 결과 임시 저장
     *
     * 정책:
     * - REQUIRES_NEW: 본 트랜잭션과 분리
     * - Post-LLM 실패해도 LLM 결과는 보존
     * - 복구 스케줄러가 이 데이터로 재처리
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun saveLlmResult(
        processingId: UUID,
        llmResultJson: String,
        commandBrandName: String,
        commandPositionName: String
    ) {
        val processing = getRequired(processingId)
        processing.saveLlmResult(llmResultJson, commandBrandName, commandPositionName)
        command.update(processing)
    }

    /**
     * 중복 JD로 파이프라인 종료
     *
     * @param reason 중복 판정 사유 (예: HASH_DUPLICATE)
     */
    @Transactional
    fun markDuplicate(
        processingId: UUID,
        reason: String
    ) {
        val processing = getRequired(processingId)
        processing.markDuplicate(reason)
        command.update(processing)
    }

    /**
     * 정상 처리 완료 상태로 전이 + JobSummary 연결
     */
    @Transactional
    fun markCompleted(processingId: UUID, summaryId: Long) {
        val processing = getRequired(processingId)
        processing.markCompleted(summaryId)
        command.update(processing)
    }

    /**
     * 실패 상태로 전이
     *
     * 주의:
     * - exception을 그대로 받지 않는다
     * - 실패 원인은 호출 측에서 "의미 있는 코드"로 명시해야 한다
     */
    @Transactional
    fun markFailed(
        processingId: UUID,
        errorCode: String,
        errorMessage: String
    ) {
        val processing = getRequired(processingId)
        processing.markFailed(
            errorCode = errorCode,
            errorMessage = errorMessage
        )
        command.update(processing)
    }

    /**
     * 반드시 존재해야 하는 Processing 조회
     */
    private fun getRequired(processingId: UUID): JdSummaryProcessing =
        query.findById(processingId)
            ?: throw IllegalStateException(
                "JdSummaryProcessing not found. id=$processingId"
            )
}
