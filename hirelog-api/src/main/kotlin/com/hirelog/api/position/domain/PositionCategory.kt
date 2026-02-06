package com.hirelog.api.position.domain

import com.hirelog.api.common.infra.jpa.entity.BaseEntity
import jakarta.persistence.*

@Entity
@Table(
    name = "position_category",
    indexes = [
        Index(
            name = "idx_position_category_normalized_name",
            columnList = "normalized_name",
            unique = true
        ),
        Index(
            name = "idx_position_category_status",
            columnList = "status"
        )
    ]
)
class PositionCategory protected constructor(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    /**
     * 사람이 인식하는 카테고리명
     * 예: "IT / Software", "Marketing"
     */
    @Column(nullable = false, length = 200)
    val name: String,

    /**
     * 시스템 식별자
     * 예: it_software, marketing
     */
    @Column(nullable = false, length = 200, unique = true)
    val normalizedName: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: PositionStatus,

    @Column(length = 500)
    val description: String?

) : BaseEntity() {

    companion object {
        fun create(name: String, description: String?): PositionCategory =
            PositionCategory(
                name = name,
                normalizedName = normalize(name),
                status = PositionStatus.ACTIVE,
                description = description
            )

        private fun normalize(value: String): String =
            value.lowercase()
                .replace(Regex("[^a-z0-9]+"), "_")
                .trim('_')
    }

    fun deactivate() {
        status = PositionStatus.INACTIVE
    }
}
