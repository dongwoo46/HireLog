package com.hirelog.api.company.domain

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "position",
    indexes = [
        Index(
            name = "idx_position_company_id_normalized_name",
            columnList = "company_id, normalized_name",
            unique = true
        )
    ]
)
class Position(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    /**
     * 소속 회사
     */
    @Column(name = "company_id", nullable = false)
    val companyId: Long,

    /**
     * 포지션 표시명
     * (사용자 입력 그대로)
     */
    @Column(name = "name", nullable = false, length = 200)
    val name: String,

    /**
     * 정규화된 포지션명
     * (검색/비교/중복 방지용)
     */
    @Column(name = "normalized_name", nullable = false, length = 200)
    val normalizedName: String,

    /**
     * 포지션에 대한 간단 설명 (선택)
     */
    @Column(name = "description", length = 500)
    val description: String? = null,

    /**
     * 포지션 최초 등록 시각
     */
    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
