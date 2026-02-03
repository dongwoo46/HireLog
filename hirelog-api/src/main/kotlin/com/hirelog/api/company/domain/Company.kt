package com.hirelog.api.company.domain

import com.hirelog.api.common.domain.VerificationStatus
import com.hirelog.api.common.infra.jpa.StringListJsonConverter
import com.hirelog.api.common.infra.jpa.entity.BaseEntity
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
class Company protected constructor(

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
     * 시스템 기준 정규화 회사명
     */
    @Column(name = "normalized_name", nullable = false, length = 200)
    val normalizedName: String,

    /**
     * 회사 별칭 목록
     *
     * 예:
     * - "토스"
     * - "Toss"
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
     * 외부 시스템 식별자
     *
     * 예:
     * - DART corpCode
     * - 사업자등록번호
     */
    @Column(name = "external_id", length = 100)
    val externalId: String?,

    /**
     * 회사 검증 상태
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 30)
    var verificationStatus: VerificationStatus,

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
         * 정책:
         * - 최초 생성 시 항상 UNVERIFIED
         * - normalizedName은 내부 규칙으로 생성
         */
        fun create(
            name: String,
            aliases: List<String>,
            source: CompanySource,
            externalId: String?
        ): Company {
            return Company(
                name = name,
                normalizedName = normalize(name),
                aliases = aliases,
                source = source,
                externalId = externalId,
                verificationStatus = VerificationStatus.UNVERIFIED,
                isActive = true
            )
        }

        fun normalize(value:String):String =
            value
                .lowercase()
                .replace(Regex("[^a-z0-9]+"), "_")
                .trim('_')
    }



    /**
     * 회사 검증 승인
     */
    fun verify() {
        if (verificationStatus == VerificationStatus.VERIFIED) return
        verificationStatus = VerificationStatus.VERIFIED
    }

    /**
     * 회사 비활성화
     */
    fun deactivate() {
        if (!isActive) return
        isActive = false
    }
}
