package com.hirelog.api.company.domain

import com.hirelog.api.common.infra.jpa.BaseEntity
import com.hirelog.api.common.infra.jpa.StringListJsonConverter
import com.hirelog.api.common.domain.VerificationStatus
import jakarta.persistence.*

@Entity
@Table(
    name = "company",
    indexes = [
        Index(
            name = "idx_company_normalized_name",
            columnList = "normalized_name",
            unique = true
        )
    ]
)
class Company(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    /**
     * 대표 회사명 (보통 법인명)
     * 예: 비바리퍼블리카
     */
    @Column(name = "name", nullable = false, length = 200)
    val name: String,

    /**
     * 정규화된 회사명
     * (중복 방지 / 매핑 기준)
     */
    @Column(name = "normalized_name", nullable = false, length = 200)
    val normalizedName: String,

    /**
     * 회사 별칭 목록
     */
    @Column(name = "aliases", columnDefinition = "jsonb", nullable = false)
    @Convert(converter = StringListJsonConverter::class)
    val aliases: List<String> = emptyList(),

    /**
     * 회사 데이터 출처
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 30)
    val source: CompanySource,

    /**
     * 회사 검증 상태
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 30)
    var verificationStatus: VerificationStatus,

    /**
     * 외부 식별자
     */
    @Column(name = "external_id", length = 100)
    val externalId: String? = null,

    /**
     * 사용 여부
     */
    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true
) : BaseEntity() {
    companion object {

        /**
         * Company 생성 팩토리
         *
         * 역할:
         * - 회사 생성 정책을 한 곳에 고정
         * - 초기 상태를 강제
         */
        fun create(
            name: String,
            normalizedName: String,
            aliases: List<String>,
            source: CompanySource,
            externalId: String?
        ): Company {
            return Company(
                name = name,
                normalizedName = normalizedName,
                aliases = aliases,
                source = source,
                verificationStatus = VerificationStatus.UNVERIFIED,
                externalId = externalId,
                isActive = true
            )
        }
    }

    /**
     * 회사 검증 승인
     *
     * 역할:
     * - 검증 상태를 VERIFIED로 전환
     * - idempotent
     */
    fun verify() {
        if (verificationStatus == VerificationStatus.VERIFIED) return
        verificationStatus = VerificationStatus.VERIFIED
    }

    /**
     * 회사 비활성화
     *
     * 역할:
     * - 논리적 삭제
     */
    fun deactivate() {
        if (!isActive) return
        isActive = false
    }
}
