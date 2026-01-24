package com.hirelog.api.job.domain

import com.hirelog.api.common.jpa.BaseEntity
import jakarta.persistence.*
import java.time.LocalDate
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import io.lettuce.core.json.JsonType
import org.hibernate.annotations.Type

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

    /**
     * 입력 소스 유형 (TEXT / OCR / URL)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, updatable = false)
    val sourceType: JobSourceType,

    /**
     * URL 입력일 경우 원본 URL
     */
    @Column(name = "source_url", length = 1000, updatable = false)
    val sourceUrl: String? = null,

    /**
     * 전처리된 JD 섹션 구조
     *
     * 예:
     * - responsibilities
     * - requirements
     * - techStack
     * - preferred
     *
     * 용도:
     * - 중복 판정
     * - JD 변화 추적
     * - 정책 기반 분석
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
        name = "canonical_sections",
        columnDefinition = "jsonb",
        nullable = false,
        updatable = false
    )
    val canonicalSections: Map<String, List<String>>,

    /**
     * pg_trgm 비교용 핵심 텍스트
     *
     * 구성:
     * - responsibilities
     * - requirements
     * - preferred
     * - process (low weight)
     *
     * 주의:
     * - 파생 데이터
     * - 단독 의미 없음
     */
    @Column(
        name = "core_text",
        columnDefinition = "text",
        nullable = false,
        updatable = false
    )
    val coreText: String,

    /**
     * 채용 기간 유형
     *
     * - FIXED / OPEN_ENDED / UNKNOWN
     * - 날짜 값과 불일치할 수 있음
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "recruitment_period_type", nullable = false, updatable = false)
    val recruitmentPeriodType: RecruitmentPeriodType,

    /**
     * 채용 지원 시작일
     *
     * - FIXED 인 경우 필수
     * - 그 외에는 null 가능
     */
    @Column(name = "opened_date", updatable = false)
    val openedDate: LocalDate?,

    /**
     * 채용 지원 마감일
     *
     * - 상시채용 / 미정일 경우 null
     */
    @Column(name = "closed_date", updatable = false)
    val closedDate: LocalDate?,

    /**
     * 완전 동일 JD 식별자
     *
     * 생성 기준:
     * - canonicalSections → deterministic flatten → SHA-256
     *
     * 용도:
     * - Fast-path 중복 제거
     * - DB unique constraint
     */
    @Column(
        name = "canonical_hash",
        nullable = false,
        length = 64,
        updatable = false
    )
    val canonicalHash: String,

    /**
     * 의미적 유사성 판정용 해시 (SimHash)
     *
     * 생성 기준:
     * - canonicalSections → weighted tokenization → SimHash
     *
     * 용도:
     * - LLM 이전 의미적 중복 판정
     * - threshold 기반 비교
     */
    @Column(
        name = "sim_hash",
        nullable = false,
        updatable = false
    )
    val simHash: Long

) : BaseEntity() {

    companion object {

        fun create(
            sourceType: JobSourceType,
            sourceUrl: String?,
            canonicalSections: Map<String, List<String>>,
            coreText: String,
            recruitmentPeriodType: RecruitmentPeriodType,
            openedDate: LocalDate?,
            closedDate: LocalDate?,
            canonicalHash: String,
            simHash: Long
        ): JobSnapshot {

            return JobSnapshot(
                sourceType = sourceType,
                sourceUrl = sourceUrl,
                canonicalSections = canonicalSections,
                coreText = coreText,
                recruitmentPeriodType = recruitmentPeriodType,
                openedDate = openedDate,
                closedDate = closedDate,
                canonicalHash = canonicalHash,
                simHash = simHash
            )
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
