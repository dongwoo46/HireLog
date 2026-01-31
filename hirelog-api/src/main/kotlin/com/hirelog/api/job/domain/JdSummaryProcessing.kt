package com.hirelog.api.job.domain

import com.hirelog.api.common.infra.jpa.entity.BaseEntity
import jakarta.persistence.*
import java.util.UUID

/**
 * JdSummaryProcessing
 *
 * 역할:
 * - 실제 요약 대상 JD의 처리 상태 기록
 * - 실패 복구 / 운영 모니터링 기준점
 *
 * 정책:
 * - 중복 JD도 운영 추적 목적이라면 Processing을 생성할 수 있음
 */
@Entity
@Table(name = "jd_summary_processing")
class JdSummaryProcessing protected constructor(

    @Id
    val id: UUID,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var status: JdSummaryProcessingStatus = JdSummaryProcessingStatus.RECEIVED,

    /**
     * 중복 판정 사유
     *
     * - status == DUPLICATE 인 경우에만 의미 있음
     */
    @Column(length = 50)
    var duplicateReason: String? = null,

    /**
     * 실패 원인 코드 (기계 판별용)
     */
    @Column(nullable = true, length = 100)
    var errorCode: String? = null,

    /**
     * 실패 상세 메시지
     *
     * - status == FAILED 인 경우에만 의미 있음
     */
    @Lob
    @Column(columnDefinition = "text")
    var errorMessage: String? = null

) : BaseEntity() {

    companion object {

        private const val ERROR_MESSAGE_MAX_LENGTH = 1000

        /**
         * Processing 생성
         *
         * 규칙:
         * - 최초 상태는 RECEIVED
         */
        fun create(id: UUID): JdSummaryProcessing =
            JdSummaryProcessing(
                id = id,
                status = JdSummaryProcessingStatus.RECEIVED
            )
    }

    /**
     * 중복 JD로 처리 종료
     *
     * 규칙:
     * - RECEIVED 상태에서만 가능
     * - duplicateReason 필수
     * - 실패 관련 필드는 초기화
     */
    fun markDuplicate(reason: String) {
        require(status == JdSummaryProcessingStatus.RECEIVED) {
            "Invalid state transition: $status -> DUPLICATE"
        }

        status = JdSummaryProcessingStatus.DUPLICATE
        duplicateReason = reason

        // 다른 상태의 잔여 데이터 제거
        errorCode = null
        errorMessage = null
    }

    /**
     * 요약 처리 시작
     */
    fun markSummarizing() {
        require(status == JdSummaryProcessingStatus.RECEIVED) {
            "Invalid state transition: $status -> SUMMARIZING"
        }

        status = JdSummaryProcessingStatus.SUMMARIZING
    }

    /**
     * 처리 완료
     *
     * 규칙:
     * - SUMMARIZING 상태에서만 가능
     * - 모든 에러/중복 정보는 제거
     */
    fun markCompleted() {
        require(status == JdSummaryProcessingStatus.SUMMARIZING) {
            "Invalid state transition: $status -> COMPLETED"
        }

        status = JdSummaryProcessingStatus.COMPLETED

        duplicateReason = null
        errorCode = null
        errorMessage = null
    }

    /**
     * 실패 처리
     *
     * 규칙:
     * - RECEIVED 또는 SUMMARIZING 상태에서 가능
     * - errorCode / errorMessage 필수
     * - 중복 관련 정보는 제거
     */
    fun markFailed(
        errorCode: String,
        errorMessage: String
    ) {
        require(
            status == JdSummaryProcessingStatus.RECEIVED ||
                    status == JdSummaryProcessingStatus.SUMMARIZING
        ) {
            "Invalid state transition: $status -> FAILED"
        }

        status = JdSummaryProcessingStatus.FAILED
        this.errorCode = errorCode
        this.errorMessage = errorMessage.take(ERROR_MESSAGE_MAX_LENGTH)

        duplicateReason = null
    }
}
