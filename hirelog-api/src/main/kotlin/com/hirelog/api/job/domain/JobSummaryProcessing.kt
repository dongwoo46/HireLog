package com.hirelog.api.job.domain

import com.hirelog.api.common.jpa.BaseEntity
import jakarta.persistence.*
import java.util.*

@Entity
@Table(name = "job_processing")
class JobSummaryProcessing protected constructor(

    @Id
    val id: UUID,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: JobSummaryProcessingStatus,

    // 연관 엔티티 참조 (단계별로 채워짐)
    var brandId: Long? = null,
    var positionId: Long? = null,
    var snapshotId: Long? = null,
    var summaryId: Long? = null,

    // 중복 판정용
    val canonicalHash: String?,

    // 실패 정보
    var errorCode: String? = null,
    var errorMessage: String? = null

) : BaseEntity() {

    companion object {

        /**
         * JobProcessing 생성
         *
         * 규칙:
         * - 최초 상태는 CREATED로 고정
         * - 외부에서 상태 지정 불가
         */
        fun create(
            id: UUID,
            canonicalHash: String?
        ): JobSummaryProcessing {
            return JobSummaryProcessing(
                id = id,
                status = JobSummaryProcessingStatus.CREATED,
                canonicalHash = canonicalHash
            )
        }
    }

    /**
     * 전처리 요청 시작
     */
    fun markPreprocessing() {
        require(status == JobSummaryProcessingStatus.CREATED) {
            "Invalid state transition: $status -> PREPROCESSING"
        }
        status = JobSummaryProcessingStatus.PREPROCESSING
    }

    /**
     * 전처리 완료
     */
    fun markPreprocessed(snapshotId: Long) {
        require(status == JobSummaryProcessingStatus.PREPROCESSING) {
            "Invalid state transition: $status -> PREPROCESSED"
        }
        this.snapshotId = snapshotId
        status = JobSummaryProcessingStatus.PREPROCESSED
    }

    /**
     * 중복 판정
     */
    fun markDuplicate() {
        require(status == JobSummaryProcessingStatus.PREPROCESSED) {
            "Invalid state transition: $status -> DUPLICATE"
        }
        status = JobSummaryProcessingStatus.DUPLICATE
    }

    /**
     * 요약 가능 상태
     */
    fun markReadyForSummary() {
        require(status == JobSummaryProcessingStatus.PREPROCESSED) {
            "Invalid state transition: $status -> READY_FOR_SUMMARY"
        }
        status = JobSummaryProcessingStatus.READY_FOR_SUMMARY
    }

    /**
     * 요약 시작
     */
    fun markSummarizing() {
        require(status == JobSummaryProcessingStatus.READY_FOR_SUMMARY) {
            "Invalid state transition: $status -> SUMMARIZING"
        }
        status = JobSummaryProcessingStatus.SUMMARIZING
    }

    /**
     * 요약 완료
     */
    fun markCompleted(summaryId: Long) {
        require(status == JobSummaryProcessingStatus.SUMMARIZING) {
            "Invalid state transition: $status -> COMPLETED"
        }
        this.summaryId = summaryId
        status = JobSummaryProcessingStatus.COMPLETED
    }

    /**
     * 실패 처리
     */
    fun markFailed(
        failedStatus: JobSummaryProcessingStatus,
        errorCode: String,
        errorMessage: String
    ) {
        require(failedStatus.name.startsWith("FAILED")) {
            "Invalid failure status: $failedStatus"
        }
        status = failedStatus
        this.errorCode = errorCode
        this.errorMessage = errorMessage
    }
}
