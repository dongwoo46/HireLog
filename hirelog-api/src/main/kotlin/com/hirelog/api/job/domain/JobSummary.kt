package com.hirelog.api.job.domain

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "job_summary",
    indexes = [
        Index(
            name = "idx_job_summary_job_snapshot_id",
            columnList = "job_snapshot_id",
            unique = true
        )
    ]
)
class JobSummary(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    /**
     * 요약 대상 JD 스냅샷
     * (JobSnapshot 1:1 관계)
     */
    @Column(name = "job_snapshot_id", nullable = false, unique = true)
    val jobSnapshotId: Long,

    /**
     * JD 전체를 한눈에 이해할 수 있는 요약 (3~5줄)
     */
    @Lob
    @Column(name = "summary_text", nullable = false)
    val summaryText: String,

    /**
     * 핵심 역할 / 담당 업무
     * (회사가 이 포지션에 기대하는 역할)
     */
    @Lob
    @Column(name = "responsibilities", nullable = false)
    val responsibilities: String,

    /**
     * 필수 요구사항 / 자격요건
     * (합격의 기준선)
     */
    @Lob
    @Column(name = "required_qualifications", nullable = false)
    val requiredQualifications: String,

    /**
     * 우대사항
     * (있으면 좋은 조건)
     */
    @Lob
    @Column(name = "preferred_qualifications")
    val preferredQualifications: String? = null,

    /**
     * 주요 기술 스택
     * (텍스트 또는 CSV 형태)
     */
    @Column(name = "tech_stack", length = 1000)
    val techStack: String? = null,

    /**
     * 채용 과정 요약
     * (예: 서류 → 과제 → 기술 면접 → 컬처핏)
     * 지원 준비 전략을 위한 정보
     */
    @Lob
    @Column(name = "recruitment_process")
    val recruitmentProcess: String? = null,

    /**
     * 요약 생성에 사용된 LLM 모델 버전
     * (예: gemini-1.5-flash)
     */
    @Column(name = "model_version", nullable = false, length = 100)
    val modelVersion: String,

    /**
     * 요약 생성 시각
     */
    @Column(name = "generated_at", nullable = false)
    val generatedAt: LocalDateTime = LocalDateTime.now()
)
