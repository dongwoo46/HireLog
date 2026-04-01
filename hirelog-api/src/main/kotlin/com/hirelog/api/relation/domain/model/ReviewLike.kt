package com.hirelog.api.relation.domain.model

import com.hirelog.api.common.infra.jpa.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "review_like",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_review_like_member_review",
            columnNames = ["member_id", "review_id"]
        )
    ],
    indexes = [
        Index(name = "idx_review_like_review", columnList = "review_id"),
        Index(name = "idx_review_like_member", columnList = "member_id")
    ]
)
class ReviewLike protected constructor(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,

    @Column(name = "member_id", nullable = false, updatable = false)
    val memberId: Long,

    @Column(name = "review_id", nullable = false, updatable = false)
    val reviewId: Long
) : BaseEntity() {
    companion object {
        fun create(memberId: Long, reviewId: Long): ReviewLike {
            require(memberId > 0) { "memberId must be positive" }
            require(reviewId > 0) { "reviewId must be positive" }
            return ReviewLike(memberId = memberId, reviewId = reviewId)
        }
    }
}
