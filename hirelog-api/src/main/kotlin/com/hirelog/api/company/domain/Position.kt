package com.hirelog.api.company.domain

import com.hirelog.api.common.jpa.BaseEntity
import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import java.time.LocalDateTime

@Entity
@Table(
    name = "position",
    indexes = [
        Index(
            name = "idx_position_brand_id_normalized_name",
            columnList = "brand_id, normalized_name",
            unique = true
        )
    ]
)
class Position(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    /**
     * 소속 브랜드
     */
    @Column(name = "brand_id", nullable = false)
    val brandId: Long,

    /**
     * 포지션 표시명
     * 예: 백엔드 개발자
     */
    @Column(name = "name", nullable = false, length = 200)
    val name: String,

    /**
     * 정규화된 포지션명
     */
    @Column(name = "normalized_name", nullable = false, length = 200)
    val normalizedName: String,

    /**
     * 포지션 설명 (선택)
     */
    @Column(name = "description", length = 500)
    val description: String? = null,

) : BaseEntity()

