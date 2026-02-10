package com.hirelog.api.job.domain.model

import com.hirelog.api.common.infra.jpa.entity.BaseEntity
import com.hirelog.api.common.logging.log
import com.hirelog.api.job.domain.type.HiringStage
import jakarta.persistence.*
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

@Entity
@Table(
    name = "job_summary_review",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_review_job_member",
            columnNames = ["job_summary_id", "member_id"]
        )
    ],
    indexes = [
        Index(name = "idx_review_job", columnList = "job_summary_id")
    ]
)
class JobSummaryReview protected constructor(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,

    /* =========================
     * Identity
     * ========================= */

    @Column(name = "job_summary_id", nullable = false, updatable = false)
    val jobSummaryId: Long,

    @Column(name = "member_id", nullable = false, updatable = false)
    val memberId: Long,

    /* =========================
     * Stage & Visibility
     * ========================= */

    @Enumerated(EnumType.STRING)
    @Column(name = "hiring_stage", nullable = false, length = 30)
    val hiringStage: HiringStage,

    @Column(name = "is_anonymous", nullable = false)
    val anonymous: Boolean,

    /* =========================
     * Ratings
     * ========================= */

    @field:Min(1)
    @field:Max(10)
    @Column(
        name = "difficulty_rating",
        nullable = false,
        columnDefinition = "INTEGER CHECK (difficulty_rating >= 1 AND difficulty_rating <= 10)"
    )
    val difficultyRating: Int,

    @field:Min(1)
    @field:Max(10)
    @Column(
        name = "satisfaction_rating",
        nullable = false,
        columnDefinition = "INTEGER CHECK (satisfaction_rating >= 1 AND satisfaction_rating <= 10)"
    )
    val satisfactionRating: Int,

    /* =========================
     * Free Text
     * ========================= */

    @Column(name = "experience_comment", columnDefinition = "TEXT", nullable = false)
    val experienceComment: String,

    @Column(name = "interview_tip", columnDefinition = "TEXT")
    val interviewTip: String? = null,

    /* =========================
     * Soft Delete
     * ========================= */

    @Column(name = "deleted", nullable = false)
    var deleted: Boolean = false

) : BaseEntity() {

    /* =========================
     * Factory
     * ========================= */

    companion object {

        fun create(
            jobSummaryId: Long,
            memberId: Long,
            hiringStage: HiringStage,
            anonymous: Boolean,
            difficultyRating: Int,
            satisfactionRating: Int,
            experienceComment: String,
            interviewTip: String?
        ): JobSummaryReview {
            log.info("[JobSummaryReview create 2]: anaymonus:{}", anonymous)
            validateRating(difficultyRating)
            validateRating(satisfactionRating)
            validateComment(experienceComment)
            validateComment(interviewTip)

            return JobSummaryReview(
                jobSummaryId = jobSummaryId,
                memberId = memberId,
                hiringStage = hiringStage,
                anonymous = anonymous,
                difficultyRating = difficultyRating,
                satisfactionRating = satisfactionRating,
                experienceComment = experienceComment,
                interviewTip = interviewTip
            )
        }

        private fun validateRating(value: Int) {
            require(value in 1..10) {
                "평점은 1~10 사이여야 합니다."
            }
        }

        private fun validateComment(text: String?) {
            require(text == null || text.length <= 2000) {
                "리뷰 텍스트는 2000자를 초과할 수 없습니다."
            }
        }
    }

    /* =========================
     * Domain Behavior
     * ========================= */

    /**
     * Soft Delete
     */
    fun softDelete() {
        this.deleted = true
    }

    /**
     * Restore (Soft Delete 복구)
     */
    fun restore() {
        this.deleted = false
    }

    fun isDeleted(): Boolean = deleted
}
