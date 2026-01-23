package com.hirelog.api.job.domain

import com.hirelog.api.common.jpa.BaseEntity
import jakarta.persistence.*
import java.security.MessageDigest
import java.time.LocalDate

@Entity
@Table(
    name = "job_snapshot",
    indexes = [
        Index(name = "idx_job_snapshot_brand_id", columnList = "brand_id"),
        Index(name = "idx_job_snapshot_position_id", columnList = "position_id"),
        Index(
            name = "uk_job_snapshot_content_hash",
            columnList = "content_hash",
        )
    ]
)
class JobSnapshot protected constructor(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,

    /**
     * 분석 완료 후 연결되는 브랜드 ID
     */
    @Column(name = "brand_id", updatable = false)
    var brandId: Long? = null,

    /**
     * 분석 완료 후 연결되는 포지션 ID
     */
    @Column(name = "position_id", updatable = false)
    var positionId: Long? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, updatable = false)
    val sourceType: JobSourceType,

    @Column(name = "source_url", length = 1000, updatable = false)
    val sourceUrl: String? = null,

    /**
     * JD 원문
     */
    @Lob
    @Column(name = "raw_text", nullable = false, updatable = false)
    val rawText: String,

    /**
     * 채용 지원 시작일
     *
     * - 중복 판정의 1차 기준
     * - 시스템이 판단한 값
     */
    @Column(name = "opened_date", updatable = false, nullable = true)
    val openedDate: LocalDate?,

    /**
     * 채용 지원 마감일
     *
     * - null이면 상시채용 또는 미정
     */
    @Column(name = "closed_date", updatable = false, nullable = true)
    val closedDate: LocalDate?,

    /**
     * JD 내용 기반 해시 (SHA-256)
     *
     * - rawText 기반
     * - 중복 판단의 유일 기준
     */
    @Column(name = "content_hash", nullable = false, length = 64, updatable = false)
    val contentHash: String

) : BaseEntity() {

    companion object {

        /**
         * JD 수집 직후 Snapshot 생성
         *
         * 규칙:
         * - contentHash는 rawText 기반 SHA-256으로 생성
         * - brand / position은 아직 연결되지 않음
         */
        fun create(
            sourceType: JobSourceType,
            sourceUrl: String?,
            rawText: String,
            contentHash: String,
            openedDate: LocalDate?,
            closedDate: LocalDate?
        ): JobSnapshot {

            return JobSnapshot(
                sourceType = sourceType,
                sourceUrl = sourceUrl,
                rawText = rawText,
                contentHash = contentHash,
                openedDate = openedDate,
                closedDate = closedDate
            )
        }

        /**
         * SHA-256 해시 생성
         *
         * 도메인 내부 전용
         */
        private fun sha256(text: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(text.toByteArray(Charsets.UTF_8))
            return hashBytes.joinToString("") { "%02x".format(it) }
        }
    }

    /**
     * 분석 완료 후 브랜드 / 포지션 연결
     *
     * 규칙:
     * - 단 한 번만 호출 가능
     */
    fun attachAnalysisResult(
        brandId: Long,
        positionId: Long
    ) {
        require(this.brandId == null && this.positionId == null) {
            "Snapshot already linked to brand/position"
        }
        this.brandId = brandId
        this.positionId = positionId
    }
}
