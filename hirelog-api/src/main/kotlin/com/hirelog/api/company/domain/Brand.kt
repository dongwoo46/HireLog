package com.hirelog.api.company.domain

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "brand",
    indexes = [
        Index(
            name = "idx_brand_normalized_name",
            columnList = "normalized_name",
            unique = true
        ),
        Index(
            name = "idx_brand_company_id",
            columnList = "company_id"
        )
    ]
)
class Brand(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    /**
     * 브랜드명
     * 예: 토스, 강남언니
     */
    @Column(name = "name", nullable = false, length = 200)
    val name: String,

    /**
     * 정규화된 브랜드명
     */
    @Column(name = "normalized_name", nullable = false, length = 200)
    val normalizedName: String,

    /**
     * 소유 회사 (검증 전에는 null 가능)
     */
    @Column(name = "company_id")
    val companyId: Long? = null,

    /**
     * 브랜드 검증 상태
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 30)
    val verificationStatus: BrandVerificationStatus,

    /**
     * 브랜드 데이터 출처
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 30)
    val source: BrandSource,

    /**
     * 생성 시각
     */
    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    /**
     * 수정 시각
     */
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {

    @PreUpdate
    fun onUpdate() {
        this.updatedAt = LocalDateTime.now()
    }
}
