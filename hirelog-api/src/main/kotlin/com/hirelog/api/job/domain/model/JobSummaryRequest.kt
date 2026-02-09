package com.hirelog.api.job.domain.model

import com.hirelog.api.common.infra.jpa.entity.BaseEntity
import com.hirelog.api.job.domain.type.JobSummaryRequestStatus
import jakarta.persistence.*
import java.util.UUID

/**
 * JobSummaryRequest
 *
 * 역할:
 * - "누가 JobSummary 생성을 요청했는가"를 기록
 * - 비동기 파이프라인 완료 시 MemberJobSummary 자동 생성의 기반 데이터
 * - 실패 시 요청자에게 알림 전달의 기반 데이터
 *
 * 정책:
 * - requestId:memberId = 1:1 (요청마다 고유 UUID 생성)
 * - requestId == JdSummaryProcessing.id.toString()
 */
@Entity
@Table(
    name = "job_summary_request",
    indexes = [
        Index(
            name = "idx_job_summary_request_request_id",
            columnList = "request_id"
        ),
        Index(
            name = "idx_job_summary_request_member_id",
            columnList = "member_id"
        )
    ]
)
class JobSummaryRequest protected constructor(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,

    @Column(name = "member_id", nullable = false, updatable = false)
    val memberId: Long,

    @Column(name = "request_id", nullable = false, updatable = false, length = 100)
    val requestId: String,

    @Column(name = "job_summary_id")
    var jobSummaryId: Long? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: JobSummaryRequestStatus = JobSummaryRequestStatus.PENDING

) : BaseEntity() {

    companion object {

        fun create(
            memberId: Long,
            requestId: String
        ): JobSummaryRequest {
            require(memberId > 0) { "memberId must be positive" }
            require(requestId.isNotBlank()) { "requestId must not be blank" }

            return JobSummaryRequest(
                memberId = memberId,
                requestId = requestId,
                status = JobSummaryRequestStatus.PENDING
            )
        }
    }

    /**
     * 요청 완료 처리
     *
     * 규칙:
     * - PENDING 상태에서만 가능
     * - jobSummaryId 필수
     */
    fun complete(jobSummaryId: Long) {
        require(status == JobSummaryRequestStatus.PENDING) {
            "Invalid state transition: $status -> COMPLETED"
        }
        require(jobSummaryId > 0) { "jobSummaryId must be positive" }

        this.status = JobSummaryRequestStatus.COMPLETED
        this.jobSummaryId = jobSummaryId
    }

    /**
     * 요청 실패 처리
     *
     * 규칙:
     * - PENDING 상태에서만 가능
     */
    fun markFailed() {
        require(status == JobSummaryRequestStatus.PENDING) {
            "Invalid state transition: $status -> FAILED"
        }

        this.status = JobSummaryRequestStatus.FAILED
    }
}
