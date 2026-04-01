package com.hirelog.api.job.domain.model

import com.hirelog.api.common.infra.jpa.entity.BaseEntity
import com.hirelog.api.common.logging.log
import com.hirelog.api.job.domain.type.HiringStage
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
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

    @Column(name = "job_summary_id", nullable = false, updatable = false)
    val jobSummaryId: Long,

    @Column(name = "member_id", nullable = false, updatable = false)
    val memberId: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "hiring_stage", nullable = false, length = 30)
    var hiringStage: HiringStage,

    @Column(name = "is_anonymous", nullable = false)
    var anonymous: Boolean,

    @field:Min(1)
    @field:Max(10)
    @Column(
        name = "difficulty_rating",
        nullable = false,
        columnDefinition = "INTEGER CHECK (difficulty_rating >= 1 AND difficulty_rating <= 10)"
    )
    var difficultyRating: Int,

    @field:Min(1)
    @field:Max(10)
    @Column(
        name = "satisfaction_rating",
        nullable = false,
        columnDefinition = "INTEGER CHECK (satisfaction_rating >= 1 AND satisfaction_rating <= 10)"
    )
    var satisfactionRating: Int,

    @Column(name = "pros_comment", columnDefinition = "TEXT", nullable = false)
    var prosComment: String,

    @Column(name = "cons_comment", columnDefinition = "TEXT", nullable = false)
    var consComment: String,

    @Column(name = "tip", columnDefinition = "TEXT")
    var tip: String? = null,

    @Column(name = "deleted", nullable = false)
    var deleted: Boolean = false
) : BaseEntity() {

    companion object {
        fun create(
            jobSummaryId: Long,
            memberId: Long,
            hiringStage: HiringStage,
            anonymous: Boolean,
            difficultyRating: Int,
            satisfactionRating: Int,
            prosComment: String,
            consComment: String,
            tip: String?
        ): JobSummaryReview {
            log.info("[JOB_SUMMARY_REVIEW_CREATE] anonymous={}", anonymous)
            validateRating(difficultyRating)
            validateRating(satisfactionRating)
            validateLongComment(prosComment)
            validateLongComment(consComment)
            validateTip(tip)

            return JobSummaryReview(
                jobSummaryId = jobSummaryId,
                memberId = memberId,
                hiringStage = hiringStage,
                anonymous = anonymous,
                difficultyRating = difficultyRating,
                satisfactionRating = satisfactionRating,
                prosComment = prosComment,
                consComment = consComment,
                tip = tip
            )
        }

        private fun validateRating(value: Int) {
            require(value in 1..10) { "평점은 1~10 사이여야 합니다." }
        }

        private fun validateLongComment(text: String) {
            require(text.length <= 2000) { "리뷰 텍스트는 2000자를 초과할 수 없습니다." }
        }

        private fun validateTip(text: String?) {
            require(text == null || text.length <= 1000) { "팁은 1000자를 초과할 수 없습니다." }
        }
    }

    fun softDelete() {
        this.deleted = true
    }

    fun restore() {
        this.deleted = false
    }

    fun update(
        hiringStage: HiringStage,
        anonymous: Boolean,
        difficultyRating: Int,
        satisfactionRating: Int,
        prosComment: String,
        consComment: String,
        tip: String?
    ) {
        validateRating(difficultyRating)
        validateRating(satisfactionRating)
        validateLongComment(prosComment)
        validateLongComment(consComment)
        validateTip(tip)

        this.hiringStage = hiringStage
        this.anonymous = anonymous
        this.difficultyRating = difficultyRating
        this.satisfactionRating = satisfactionRating
        this.prosComment = prosComment
        this.consComment = consComment
        this.tip = tip
    }

    fun isDeleted(): Boolean = deleted
}
