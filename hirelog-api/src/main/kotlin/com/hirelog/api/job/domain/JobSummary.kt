package com.hirelog.api.job.domain

import com.hirelog.api.common.jpa.BaseEntity
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
    @Column(name = "job_snapshot_id", nullable = false)
    val jobSnapshotId: Long,

    // =========================
    // 🔥 핵심 비정규화 필드
    // =========================

    /** JD 기준 브랜드 */
    @Column(name = "brand_id", nullable = false)
    val brandId: Long,

    @Column(name = "brand_name", nullable = false, length = 200)
    val brandName: String,

    /** 소속 법인 (없을 수도 있음) */
    @Column(name = "company_id")
    val companyId: Long? = null,

    @Column(name = "company_name", length = 200)
    val companyName: String? = null,

    @Column(name="position_id", nullable = false)
    val positionId: Long,

    /** 포지션 (Brand 종속 개념) */
    @Column(name = "position_name", nullable = false, length = 200)
    val positionName: String,

    /**
     * 채용 경력 유형
     * (신입 / 경력 / 무관)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "career_type", nullable = false, length = 20)
    val careerType: CareerType,

    /**
     * 최소 경력 연차
     * - 신입 / 무관 / 미기재 → null
     * - "3년 이상" → 3
     */
    @Column(name = "career_years")
    val careerYears: Int? = null,

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

) : BaseEntity()
