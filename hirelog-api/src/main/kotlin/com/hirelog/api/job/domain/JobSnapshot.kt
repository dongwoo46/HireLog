package com.hirelog.api.job.domain

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "job_snapshot",
    indexes = [
        Index(name = "idx_job_snapshot_brand_id", columnList = "brand_id"),
        Index(name = "idx_job_snapshot_company_id", columnList = "company_id"),
        Index(name = "idx_job_snapshot_position_id", columnList = "position_id"),
        Index(name = "idx_job_snapshot_hash", columnList = "content_hash", unique = true)
    ]
)
class JobSnapshot(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    /**
     * 채용 주체 브랜드
     * (JD 기준 식별자)
     */
    @Column(name = "brand_id", nullable = false)
    val brandId: Long,

    /**
     * 법적 회사 (분석/정합성용)
     */
    @Column(name = "company_id", nullable = true)
    val companyId: Long? = null,

    /**
     * 포지션
     * (Brand 기준 포지션)
     */
    @Column(name = "position_id", nullable = false)
    val positionId: Long,

    /**
     * JD 수집 방식
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false)
    val sourceType: JobSourceType,

    /**
     * JD 원본 URL
     */
    @Column(name = "source_url", length = 1000)
    val sourceUrl: String? = null,

    /**
     * JD 원문 텍스트
     */
    @Lob
    @Column(name = "raw_text", nullable = false)
    val rawText: String,

    /**
     * 중복 방지 해시
     * (brand + position + canonical_text 기준)
     */
    @Column(name = "content_hash", nullable = false, length = 64)
    val contentHash: String,

    /**
     * 스냅샷 생성 시각
     */
    @Column(name = "captured_at", nullable = false)
    val capturedAt: LocalDateTime = LocalDateTime.now()
)

