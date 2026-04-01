package com.hirelog.api.relation.infra.persistence.jpa.repository

import com.hirelog.api.relation.domain.model.ReviewLike
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ReviewLikeJpaRepository : JpaRepository<ReviewLike, Long> {
    fun findByMemberIdAndReviewId(memberId: Long, reviewId: Long): ReviewLike?
    fun existsByMemberIdAndReviewId(memberId: Long, reviewId: Long): Boolean
    fun countByReviewId(reviewId: Long): Long

    interface ReviewLikeStatRow {
        fun getLikeCount(): Long
        fun getLikedByMe(): Boolean
    }

    @Query(
        value = """
            SELECT
                COUNT(*) AS likeCount,
                EXISTS (
                    SELECT 1
                    FROM review_like rl2
                    WHERE rl2.review_id = :reviewId
                      AND rl2.member_id = :memberId
                ) AS likedByMe
            FROM review_like rl
            WHERE rl.review_id = :reviewId
        """,
        nativeQuery = true
    )
    fun findStat(
        @Param("reviewId") reviewId: Long,
        @Param("memberId") memberId: Long
    ): ReviewLikeStatRow
}
