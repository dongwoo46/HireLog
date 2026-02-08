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
        ),
        Index(
            name = "idx_job_summary_source_url",
            columnList = "source_url"
        ),
        Index(
            name = "idx_job_summary_is_active",
            columnList = "is_active"
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
    var companyId: Long? = null,

    /**
     * 회사명
     */
    @Column(name = "company_name", length = 200, updatable = false)
    var companyName: String? = null,

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
     * BrandPosition 식별자
     * Brand-Position 매핑 엔티티 참조
     */
    @Column(name = "brand_position_id", nullable = false, updatable = false)
    val brandPositionId: Long,

    /**
     * 브랜드 내부 포지션명 (JD에 명시된 원본)
     * 예: "서버 개발자 (결제팀)"
     */
    @Column(name = "brand_position_name", length = 300, nullable = false, updatable = false)
    val brandPositionName: String,

    /**
     * 포지션 카테고리 식별자 (비정규화)
     * 검색 필터링 최적화용
     */
    @Column(name = "position_category_id", nullable = false, updatable = false)
    val positionCategoryId: Long,

    /**
     * 포지션 카테고리명 (비정규화)
     * 검색/조회 최적화용
     */
    @Column(name = "position_category_name", nullable = false, length = 100, updatable = false)
    val positionCategoryName: String,

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
    val llmModel: String,

    /**
     * 원본 JD URL (URL 소스인 경우만)
     * TEXT/OCR 소스는 null
     */
    @Column(name = "source_url", length = 2000, updatable = false)
    val sourceUrl: String? = null,

    // =========================
    // 상태 관리
    // =========================

    /**
     * 활성화 상태
     *
     * 정책:
     * - 기본값: true (활성화)
     * - 비활성화 시 일반 조회에서 제외
     * - Admin 조회에서는 모두 표시 가능
     */
    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true

) : VersionedEntity() {

    /**
     * 비활성화 처리
     *
     * 용도:
     * - 잘못된 데이터 숨김
     * - 중복 데이터 처리
     * - 삭제 대신 소프트 삭제
     */
    fun deactivate() {
        if (!isActive) return
        isActive = false
    }

    /**
     * 재활성화 처리
     *
     * 용도:
     * - 잘못 비활성화된 데이터 복구
     */
    fun activate() {
        if (isActive) return
        isActive = true
    }

    fun applyCompany(
        companyId: Long,
        companyName: String
    ) {
        if (this.companyId != null) return  // 이미 반영된 경우 무시
        this.companyId = companyId
        this.companyName = companyName
    }

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
            brandPositionId: Long,
            brandPositionName: String,
            positionCategoryId: Long,
            positionCategoryName: String,
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
            llmModel: String,
            sourceUrl: String? = null
        ): JobSummary {
            return JobSummary(
                jobSnapshotId = jobSnapshotId,
                brandId = brandId,
                brandName = brandName,
                companyId = companyId,
                companyName = companyName,
                positionId = positionId,
                positionName = positionName,
                brandPositionId = brandPositionId,
                brandPositionName = brandPositionName,
                positionCategoryId = positionCategoryId,
                positionCategoryName = positionCategoryName,
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
                llmModel = llmModel,
                sourceUrl = sourceUrl,
                isActive = true
            )
        }
    }
}
