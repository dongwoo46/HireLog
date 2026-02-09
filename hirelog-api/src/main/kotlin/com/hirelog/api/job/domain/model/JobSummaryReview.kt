package com.hirelog.api.job.domain.model

import com.hirelog.api.common.infra.jpa.entity.BaseEntity
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
    var hiringStage: HiringStage,

    @Column(name = "is_anonymous", nullable = false)
    var anonymous: Boolean,

    /* =========================
     * Ratings
     * ========================= */

    @field:Min(1) // 애플리케이션 레벨 검증 (Hibernate Validator)
    @field:Max(10)
    @Column(
        name = "difficulty_rating",
        nullable = false,
        columnDefinition = "INTEGER CHECK (difficulty_rating >= 1 AND difficulty_rating <= 10)" // DB 레벨 제약 조건
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

    /* =========================
     * Free Text
     * ========================= */

    @Column(name = "experience_comment", columnDefinition = "TEXT", nullable = false)
    var experienceComment: String,

    @Column(name = "interview_tip", columnDefinition = "TEXT")
    var interviewTip: String? = null

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
     * 리뷰 수정
     *
     * 규칙:
     * - 작성자만 수정 가능 (Application Layer에서 보장)
     * - 식별자 / 대상 JD는 변경 불가
     */
    fun update(
        hiringStage: HiringStage,
        anonymous: Boolean,
        difficultyRating: Int,
        satisfactionRating: Int,
        experienceComment: String,
        interviewTip: String?
    ) {
        validateRating(difficultyRating)
        validateRating(satisfactionRating)
        validateComment(experienceComment)
        validateComment(interviewTip)

        this.hiringStage = hiringStage
        this.anonymous = anonymous
        this.difficultyRating = difficultyRating
        this.satisfactionRating = satisfactionRating
        this.experienceComment = experienceComment
        this.interviewTip = interviewTip
    }

    /**
     * 익명 여부 확인
     */
    fun isAnonymous(): Boolean = anonymous

    /**
     * 외부 노출용 작성자 식별자
     *
     * - 익명인 경우 null 반환
     */
    fun getPublicMemberId(): Long? =
        if (anonymous) null else memberId
}
