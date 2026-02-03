package com.hirelog.api.job.domain

import com.hirelog.api.common.domain.LlmProvider
import com.hirelog.api.common.infra.jpa.entity.BaseEntity
import com.hirelog.api.common.infra.jpa.entity.VersionedEntity
import jakarta.persistence.*

/**
 * Job Description 요약 엔티티 (Read Model)
 *
 * 역할:
 * - JobSnapshot을 기반으로 생성된 "조회 전용 요약 데이터"
 * - 검색, 리스트, 상세 조회에 최적화된 형태
 *
 * 설계 의도:
 * - Snapshot(원문)은 무겁고 변경 불가
 * - Summary는 비정규화를 통해 조회 비용을 최소화
 *
 * 제약:
 * - JobSnapshot과 1:1 관계
 * - 생성 이후 수정되지 않는다
 */
@Entity
@Table(
    name = "job_summary",
    indexes = [
        Index(
            name = "uk_job_summary_snapshot_id",
            columnList = "job_snapshot_id",
            unique = true
        )
    ]
)
class JobSummary protected constructor(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,

    /**
     * 요약 대상 JobSnapshot 식별자
     *
     * 주의:
     * - JPA 연관관계는 의도적으로 사용하지 않는다
     * - Summary는 Snapshot을 참조만 하며 생명주기를 공유하지 않는다
     */
    @Column(name = "job_snapshot_id", nullable = false, updatable = false)
    val jobSnapshotId: Long,

    // =========================
    // 비정규화 필드
    // =========================

    /**
     * JD 기준 브랜드 식별자
     * 조회 조건 최적화를 위해 중복 저장
     */
    @Column(name = "brand_id", nullable = false, updatable = false)
    val brandId: Long,

    /**
     * 브랜드명
     * Snapshot 이후 변경되어도 Summary는 고정
     */
    @Column(name = "brand_name", nullable = false, length = 200, updatable = false)
    val brandName: String,

    /**
     * 법적 회사 식별자
     * 없는 경우 null
     */
    @Column(name = "company_id", updatable = false)
    val companyId: Long? = null,

    /**
     * 회사명
     */
    @Column(name = "company_name", length = 200, updatable = false)
    val companyName: String? = null,

    /**
     * 브랜드 기준 포지션 식별자
     */
    @Column(name = "position_id", nullable = false, updatable = false)
    val positionId: Long,

    /**
     * 포지션명 (시스템 정규화된 이름)
     */
    @Column(name = "position_name", nullable = false, length = 200, updatable = false)
    val positionName: String,

    /**
     * 브랜드 내부 포지션명 (JD에 명시된 원본)
     * 예: "서버 개발자 (결제팀)"
     */
    @Column(name = "brand_position_name", length = 300, updatable = false)
    val brandPositionName: String? = null,

    /**
     * 채용 경력 유형
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "career_type", nullable = false, length = 20, updatable = false)
    val careerType: CareerType,

    /**
     * 경력 연차 원문
     * 예: "3년 이상", "5~7년", "신입"
     * 미기재 시 null
     */
    @Column(name = "career_years", length = 50, updatable = false)
    val careerYears: String? = null,

    /**
     * JD 전체 요약
     * 2~3줄 분량
     */
    @Column(
        name = "summary_text",
        nullable = false,
        updatable = false,
        columnDefinition = "TEXT"
    )
    val summaryText: String,

    /**
     * 핵심 역할 / 담당 업무 요약
     */
    @Column(
        name = "responsibilities",
        nullable = false,
        updatable = false,
        columnDefinition = "TEXT"
    )
    val responsibilities: String,

    /**
     * 필수 자격 요건
     */
    @Column(
        name = "required_qualifications",
        nullable = false,
        updatable = false,
        columnDefinition = "TEXT"
    )
    val requiredQualifications: String,

    /**
     * 우대 사항
     */
    @Lob
    @Column(name = "preferred_qualifications", updatable = false)
    val preferredQualifications: String? = null,

    /**
     * 주요 기술 스택
     * CSV 형태, 정규화된 영문명
     */
    @Column(name = "tech_stack", length = 1000, updatable = false)
    val techStack: String? = null,

    /**
     * 채용 과정 요약
     */
    @Lob
    @Column(name = "recruitment_process", updatable = false)
    val recruitmentProcess: String? = null,

    // =========================
    // Insight (Embedded VO)
    // =========================

    /**
     * JD 분석 기반 인사이트
     */
    @Embedded
    val insight: JobSummaryInsight,

    // =========================
    // LLM 메타 정보
    // =========================

    /**
     * 요약 생성에 사용된 LLM Provider
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "llm_provider", nullable = false, length = 30, updatable = false)
    val llmProvider: LlmProvider,

    /**
     * 요약 생성에 사용된 LLM 모델
     */
    @Column(name = "llm_model", nullable = false, length = 50, updatable = false)
    val llmModel: String

) : VersionedEntity() {

    companion object {
        /**
         * JobSummary 생성 전용 팩토리 메서드
         */
        fun create(
            jobSnapshotId: Long,
            brandId: Long,
            brandName: String,
            companyId: Long?,
            companyName: String?,
            positionId: Long,
            positionName: String,
            brandPositionName: String?,
            careerType: CareerType,
            careerYears: String?,
            summaryText: String,
            responsibilities: String,
            requiredQualifications: String,
            preferredQualifications: String?,
            techStack: String?,
            recruitmentProcess: String?,
            insight: JobSummaryInsight,
            llmProvider: LlmProvider,
            llmModel: String
        ): JobSummary {
            return JobSummary(
                jobSnapshotId = jobSnapshotId,
                brandId = brandId,
                brandName = brandName,
                companyId = companyId,
                companyName = companyName,
                positionId = positionId,
                positionName = positionName,
                brandPositionName = brandPositionName,
                careerType = careerType,
                careerYears = careerYears,
                summaryText = summaryText,
                responsibilities = responsibilities,
                requiredQualifications = requiredQualifications,
                preferredQualifications = preferredQualifications,
                techStack = techStack,
                recruitmentProcess = recruitmentProcess,
                insight = insight,
                llmProvider = llmProvider,
                llmModel = llmModel
            )
        }
    }
}
