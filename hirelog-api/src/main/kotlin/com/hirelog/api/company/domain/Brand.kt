package com.hirelog.api.company.domain

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
        )
    ]
)
class Brand(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    /**
     * 브랜드명
     * 예: 토스
     */
    @Column(name = "name", nullable = false, length = 200)
    val name: String,

    /**
     * 정규화된 브랜드명
     */
    @Column(name = "normalized_name", nullable = false, length = 200)
    val normalizedName: String,

    /**
     * 소유 회사
     */
    @Column(name = "company_id", nullable = false)
    val companyId: Long
)
