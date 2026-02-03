package com.hirelog.api.company.domain

import com.hirelog.api.common.infra.jpa.entity.BaseEntity
import com.hirelog.api.common.infra.jpa.entity.VersionedEntity
import jakarta.persistence.*

@Entity
@Table(
    name = "company_candidate",
    indexes = [
        Index(
            name = "idx_company_candidate_brand",
            columnList = "brand_id"
        ),
        Index(
            name = "idx_company_candidate_status",
            columnList = "status"
        )
    ]
)
class CompanyCandidate protected constructor(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    /**
     * 근거가 된 JD Summary
     */
    @Column(name = "jd_summary_id", nullable = false)
    val jdSummaryId: Long,

    /**
     * 후보가 귀속된 Brand
     */
    @Column(name = "brand_id", nullable = false)
    val brandId: Long,

    /**
     * 추정된 법인명 (원문 그대로)
     */
    @Column(name = "candidate_name", nullable = false, length = 200)
    val candidateName: String,

    /**
     * 시스템 기준 정규화 법인명
     */
    @Column(name = "normalized_name", nullable = false, length = 200)
    val normalizedName: String,

    /**
     * 추정 출처
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 30)
    val source: CompanyCandidateSource,

    /**
     * 신뢰도 점수 (0.0 ~ 1.0)
     */
    @Column(name = "confidence_score", nullable = false)
    val confidenceScore: Double,

    /**
     * 처리 상태
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    var status: CompanyCandidateStatus

) : VersionedEntity() {

    companion object {

        /**
         * CompanyCandidate 생성 팩토리
         *
         * 정책:
         * - 항상 PENDING 상태로 생성
         * - normalizedName은 내부 규칙으로 생성
         */
        fun create(
            jdSummaryId: Long,
            brandId: Long,
            candidateName: String,
            source: CompanyCandidateSource,
            confidenceScore: Double,
        ): CompanyCandidate {
            return CompanyCandidate(
                jdSummaryId = jdSummaryId,
                brandId = brandId,
                candidateName = candidateName,
                normalizedName = normalize(candidateName),
                source = source,
                confidenceScore = confidenceScore,
                status = CompanyCandidateStatus.PENDING
            )
        }

        /**
         * 법인명 정규화 규칙
         *
         * 책임:
         * - 외부 입력을 시스템 식별자 형태로 변환
         */
        private fun normalize(value: String): String =
            value
                .lowercase()
                .replace(Regex("[^a-z0-9]+"), "_")
                .trim('_')
    }

    /**
     * 후보 승인
     *
     * 정책:
     * - PENDING → APPROVED만 허용
     */
    fun approve() {
        if (status != CompanyCandidateStatus.PENDING) return
        status = CompanyCandidateStatus.APPROVED
    }

    /**
     * 후보 거절
     *
     * 정책:
     * - 언제든 REJECTED 가능
     */
    fun reject() {
        if (status == CompanyCandidateStatus.REJECTED) return
        status = CompanyCandidateStatus.REJECTED
    }
}
