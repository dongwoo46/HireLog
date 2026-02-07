package com.hirelog.api.brand.domain

import com.hirelog.api.common.infra.jpa.entity.BaseEntity
import com.hirelog.api.common.domain.VerificationStatus
import com.hirelog.api.common.utils.Normalizer
import jakarta.persistence.*

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
        ),
        Index(
            name = "idx_brand_verification_status_created_at",
            columnList = "verification_status, created_at"
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
    @Column(name = "name", nullable = false, length = 100)
    val name: String,

    /**
     * 정규화된 브랜드명
     */
    @Column(name = "normalized_name", nullable = false, length = 100)
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
    var verificationStatus: VerificationStatus,

    /**
     * 브랜드 데이터 출처
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 20)
    val source: BrandSource,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true
) : BaseEntity() {

    companion object {

        fun create(
            name: String,
            companyId: Long?,
            source: BrandSource
        ): Brand {
            return Brand(
                name = name,
                normalizedName = Normalizer.normalizeBrand(name),
                companyId = companyId,
                verificationStatus = VerificationStatus.UNVERIFIED,
                source = source,
                isActive = true
            )
        }

        fun createByAdmin(
            name: String,
            companyId: Long?,
        ): Brand {
            return Brand(
                name = name,
                normalizedName = Normalizer.normalizeBrand(name),
                companyId = companyId,
                verificationStatus = VerificationStatus.VERIFIED,
                source = BrandSource.ADMIN,
                isActive = true
            )
        }
    }

    /**
     * 브랜드 검증 승인
     */
    fun verify() {
        if (verificationStatus == VerificationStatus.VERIFIED) return
        verificationStatus = VerificationStatus.VERIFIED
        isActive = true
    }

    /**
     * 브랜드 검증 거절
     */
    fun reject() {
        if (verificationStatus == VerificationStatus.REJECTED) return

        verificationStatus = VerificationStatus.REJECTED
        isActive = false
    }

    /**
     * 브랜드 비활성화
     */
    fun deactivate() {
        if (!isActive) return
        isActive = false
    }

    /**
     * 브랜드 활성화
     */
    fun activate() {
        require(verificationStatus == VerificationStatus.VERIFIED) {
            "Only verified brand can be activated (current=$verificationStatus)"
        }

        if (isActive) return
        isActive = true
    }
}
