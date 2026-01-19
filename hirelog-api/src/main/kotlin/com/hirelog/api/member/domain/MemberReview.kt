package com.hirelog.api.member.domain

import com.hirelog.api.common.jpa.BaseEntity
import com.hirelog.api.member.domain.vo.ReviewContent
import jakarta.persistence.*

@Entity
@Table(
    name = "member_review",
    indexes = [
        // 사용자가 작성한 리뷰 조회
        Index(
            name = "idx_member_review_member",
            columnList = "member_id"
        ),
        // JD 요약별 리뷰 조회
        Index(
            name = "idx_member_review_job_summary",
            columnList = "job_summary_id"
        ),
        // 한 사용자는 하나의 JD에 하나의 리뷰만
        Index(
            name = "ux_member_review_member_job_summary",
            columnList = "member_id, job_summary_id",
            unique = true
        )
    ]
)
class MemberReview(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    /**
     * 리뷰 작성자
     */
    @Column(name = "member_id", nullable = false)
    val memberId: Long,

    /**
     * 리뷰 대상 JD 요약
     */
    @Column(name = "job_summary_id", nullable = false)
    val jobSummaryId: Long,

    /**
     * 리뷰 본문 (VO)
     */
    @Embedded
    val content: ReviewContent,

    /**
     * 공개 여부
     * - false: 나만 보기
     * - true : 공개 (추후 커뮤니티 확장)
     */
    @Column(name = "is_public", nullable = false)
    val isPublic: Boolean = false

) : BaseEntity()
