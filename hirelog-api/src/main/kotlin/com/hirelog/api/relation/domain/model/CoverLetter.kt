package com.hirelog.api.relation.domain.model

import jakarta.persistence.*

@Entity
@Table(
    name = "cover_letter",
    indexes = [
        Index(
            name = "idx_cover_letter_member_job_summary",
            columnList = "member_job_summary_id"
        )
    ]
)
class CoverLetter(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(
        name = "member_job_summary_id",
        nullable = false,
        insertable = false,
        updatable = false
    )
    val memberJobSummaryId: Long,

    @Column(name = "question", nullable = false, columnDefinition = "TEXT")
    var question: String,

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    var content: String,

    @Column(name = "sort_order", nullable = false)
    var sortOrder: Int = 0
)
