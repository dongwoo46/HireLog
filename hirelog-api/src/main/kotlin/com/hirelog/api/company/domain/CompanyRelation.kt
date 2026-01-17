package com.hirelog.api.company.domain

import com.hirelog.api.common.jpa.BaseEntity
import jakarta.persistence.*

@Entity
@Table(
    name = "company_relation",
    indexes = [
        Index(
            name = "idx_company_relation_parent_child",
            columnList = "parent_company_id, child_company_id",
            unique = true
        ),
        Index(
            name = "idx_company_relation_child",
            columnList = "child_company_id"
        )
    ]
)
class CompanyRelation(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    /**
     * 모회사
     */
    @Column(name = "parent_company_id", nullable = false)
    val parentCompanyId: Long,

    /**
     * 자회사 / 관계 대상 회사
     */
    @Column(name = "child_company_id", nullable = false)
    val childCompanyId: Long,

    /**
     * 관계 유형
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "relation_type", nullable = false, length = 30)
    val relationType: CompanyRelationType,

) : BaseEntity()
