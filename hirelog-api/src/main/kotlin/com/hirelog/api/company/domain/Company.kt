package com.hirelog.api.company.domain

import com.hirelog.api.common.jpa.BaseEntity
import com.hirelog.api.common.jpa.StringListJsonConverter
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
    @Column(name = "company_verification_status", nullable = false, length = 30)
    val verificationStatus: CompanyVerificationStatus,

    /**
     * 외부 식별자
     */
    @Column(name = "external_id", length = 100)
    val externalId: String? = null,

    /**
     * 사용 여부
     */
    @Column(name = "is_active", nullable = false)
    val isActive: Boolean = true
) :BaseEntity()
