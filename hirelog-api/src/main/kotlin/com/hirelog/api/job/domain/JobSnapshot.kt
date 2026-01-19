package com.hirelog.api.job.domain

import com.hirelog.api.common.jpa.BaseEntity
import jakarta.persistence.*

/**
 * Job Description 스냅샷 엔티티
 *
 * 역할:
 * - 특정 시점에 수집된 JD 원문을 그대로 보존
 * - 이후 변경/삭제 없이 히스토리로만 관리
 *
 * 설계 의도:
 * - 동일 브랜드 + 포지션이라도 시점/내용이 다르면 다른 Snapshot
 * - 중복 판단은 contentHash 기준으로만 수행
 */
@Entity
@Table(
    name = "job_snapshot",
    indexes = [
        Index(name = "idx_job_snapshot_brand_id", columnList = "brand_id"),
        Index(name = "idx_job_snapshot_company_id", columnList = "company_id"),
        Index(name = "idx_job_snapshot_position_id", columnList = "position_id"),
        Index(
            name = "uk_job_snapshot_content_hash",
            columnList = "content_hash",
            unique = true
        )
    ]
)
class JobSnapshot protected constructor(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,

    /**
     * 채용 브랜드 식별자
     * JD 관점에서의 주체
     */
    @Column(name = "brand_id", nullable = false, updatable = false)
    val brandId: Long,

    /**
     * 법적 회사 식별자
     * 분석/정합성 용도로만 사용
     */
    @Column(name = "company_id", updatable = false)
    val companyId: Long? = null,

    /**
     * 브랜드 기준 포지션 식별자
     */
    @Column(name = "position_id", nullable = false, updatable = false)
    val positionId: Long,

    /**
     * JD 수집 소스 타입
     * (URL / OCR / API 등)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, updatable = false)
    val sourceType: JobSourceType,

    /**
     * JD 원본 URL
     * URL 기반 수집이 아닌 경우 null 허용
     */
    @Column(name = "source_url", length = 1000, updatable = false)
    val sourceUrl: String? = null,

    /**
     * JD 원문 텍스트
     *
     * 주의:
     * - 절대 수정되지 않는다
     * - 후처리/정제는 별도 파이프라인 책임
     */
    @Lob
    @Column(name = "raw_text", nullable = false, updatable = false)
    val rawText: String,

    /**
     * 중복 판별용 해시
     *
     * 생성 규칙:
     * - brandId + positionId + canonicalText
     *
     * 제약:
     * - 전역 유니크
     */
    @Column(name = "content_hash", nullable = false, length = 64, updatable = false)
    val contentHash: String

) : BaseEntity() {

    companion object {
        /**
         * JobSnapshot 생성 전용 팩토리 메서드
         *
         * 목적:
         * - 생성 규칙을 한 곳으로 집중
         * - 향후 hash 생성 로직 변경 대비
         */
        fun create(
            brandId: Long,
            companyId: Long?,
            positionId: Long,
            sourceType: JobSourceType,
            sourceUrl: String?,
            rawText: String,
            contentHash: String
        ): JobSnapshot {
            return JobSnapshot(
                brandId = brandId,
                companyId = companyId,
                positionId = positionId,
                sourceType = sourceType,
                sourceUrl = sourceUrl,
                rawText = rawText,
                contentHash = contentHash
            )
        }
    }
}
