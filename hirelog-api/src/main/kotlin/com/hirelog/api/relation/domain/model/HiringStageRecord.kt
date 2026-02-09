package com.hirelog.api.relation.domain.model

import com.hirelog.api.job.domain.type.HiringStage
import com.hirelog.api.relation.domain.type.HiringStageResult
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "member_job_summary_stage",
    uniqueConstraints = [
        UniqueConstraint(
            name = "ux_member_job_summary_stage",
            columnNames = ["member_job_summary_id", "stage"]
        )
    ]
)
class HiringStageRecord(

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

    @Enumerated(EnumType.STRING)
    @Column(name = "stage", nullable = false, length = 30)
    val stage: HiringStage,

    /**
     * 사용자 경험 기록
     * - 질문
     * - 분위기
     * - 난이도
     * - 느낀 점
     */
    @Column(name = "note", nullable = false, length = 2000)
    var note: String,

    /**
     * 단계 결과 (선택)
     * - PASSED / FAILED
     * - 모르면 null
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "result", nullable = true, length = 20)
    var result: HiringStageResult? = null,

    @Column(name = "recorded_at", nullable = false)
    var recordedAt: LocalDateTime = LocalDateTime.now()
)
