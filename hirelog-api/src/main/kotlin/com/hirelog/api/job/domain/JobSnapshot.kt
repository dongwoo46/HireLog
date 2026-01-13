package com.hirelog.api.job.domain

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "job_snapshot",
    indexes = [
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
     * 소속 회사
     */
    @Column(name = "company_id", nullable = false)
    val companyId: Long,

    /**
     * 회사 내 포지션 (기업 종속)
     */
    @Column(name = "position_id", nullable = false)
    val positionId: Long,

    /**
     * JD 수집 방식
     * URL / IMAGE / TEXT
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false)
    val sourceType: JobSourceType,

    /**
     * JD 원본 URL (이미지/텍스트 입력일 경우 null)
     */
    @Column(name = "source_url", length = 1000)
    val sourceUrl: String? = null,

    /**
     * JD 원문 텍스트 (크롤링 / OCR 결과)
     */
    @Lob
    @Column(name = "raw_text", nullable = false)
    val rawText: String,

    /**
     * 동일 JD 중복 방지를 위한 해시
     * (rawText 기반)
     */
    @Column(name = "content_hash", nullable = false, length = 64)
    val contentHash: String,

    /**
     * 스냅샷 생성 시각
     */
    @Column(name = "captured_at", nullable = false)
    val capturedAt: LocalDateTime = LocalDateTime.now()
)
