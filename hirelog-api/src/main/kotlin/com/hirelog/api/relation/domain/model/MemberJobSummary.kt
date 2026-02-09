package com.hirelog.api.relation.domain.model

import com.hirelog.api.common.infra.jpa.entity.BaseEntity
import com.hirelog.api.job.domain.type.HiringStage
import com.hirelog.api.relation.domain.type.HiringStageResult
import com.hirelog.api.relation.domain.type.MemberJobSummarySaveType
import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * MemberJobSummary (Aggregate Root)
 *
 * 의미:
 * - 사용자가 특정 JobSummary를 저장/관리하는 "개인화된 채용 공고 스냅샷"
 *
 * 핵심 설계:
 * - JobSummary의 핵심 식별 정보는 생성 시점에 비정규화되어 고정된다.
 * - 채용 단계 기록(HiringStageRecord)은 이 Aggregate에 완전히 종속된다.
 */
@Entity
@Table(
    name = "member_job_summary",
    indexes = [
        Index(
            name = "idx_member_job_summary_job_summary",
            columnList = "job_summary_id"
        ),
        Index(
            name = "ux_member_job_summary_member_job",
            columnList = "member_id, job_summary_id",
            unique = true
        )
    ]
)
class MemberJobSummary private constructor(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,

    @Column(name = "member_id", nullable = false)
    val memberId: Long,

    @Column(name = "job_summary_id", nullable = false)
    val jobSummaryId: Long,

    /* =========================
     * 비정규화 스냅샷 필드
     * ========================= */

    @Column(name = "brand_name", nullable = false, length = 200)
    val brandName: String,

    @Column(name = "position_name", nullable = false, length = 200)
    val positionName: String,

    @Column(name = "brand_position_name", nullable = false, length = 300)
    val brandPositionName: String,

    @Column(name = "position_category_name", nullable = false, length = 100)
    val positionCategoryName: String,

    /* =========================
     * 상태
     * ========================= */

    @Enumerated(EnumType.STRING)
    @Column(name = "save_type", nullable = false, length = 20)
    var saveType: MemberJobSummarySaveType,

    /* =========================
     * 채용 단계 기록
     * ========================= */

    @OneToMany(
        fetch = FetchType.LAZY,
        cascade = [CascadeType.ALL],
        orphanRemoval = true
    )
    @JoinColumn(name = "member_job_summary_id", nullable = false)
    private val stageRecords: MutableList<HiringStageRecord> = mutableListOf()

) : BaseEntity() {

    companion object {

        /**
         * 최초 저장 생성 팩토리
         *
         * 정책:
         * - 생성 시점의 JobSummary 정보를 스냅샷으로 고정
         * - 초기 상태는 항상 SAVED
         */
        fun create(
            memberId: Long,
            jobSummaryId: Long,
            brandName: String,
            positionName: String,
            brandPositionName: String,
            positionCategoryName: String
        ): MemberJobSummary {
            return MemberJobSummary(
                memberId = memberId,
                jobSummaryId = jobSummaryId,
                brandName = brandName,
                positionName = positionName,
                brandPositionName = brandPositionName,
                positionCategoryName = positionCategoryName,
                saveType = MemberJobSummarySaveType.SAVED
            )
        }
    }

    /* =========================
     * 상태 전이
     * ========================= */

    fun changeStatus(target: MemberJobSummarySaveType) {
        when (target) {
            MemberJobSummarySaveType.SAVED -> {
                require(saveType != MemberJobSummarySaveType.UNSAVED) {
                    "Archived summary must be restored explicitly"
                }
                saveType = MemberJobSummarySaveType.SAVED
            }

            MemberJobSummarySaveType.APPLY -> {
                require(saveType != MemberJobSummarySaveType.UNSAVED) {
                    "Archived summary cannot be set to APPLY directly"
                }
                saveType = MemberJobSummarySaveType.APPLY
            }

            MemberJobSummarySaveType.UNSAVED -> {
                saveType = MemberJobSummarySaveType.UNSAVED
            }
        }
    }

    /* =========================
     * 채용 단계 관리
     * ========================= */

    fun addStageRecord(stage: HiringStage, note: String) {
        require(saveType == MemberJobSummarySaveType.APPLY) {
            "Stage records can be managed only in APPLY state"
        }
        require(note.isNotBlank()) {
            "Stage note must not be blank"
        }
        require(stageRecords.none { it.stage == stage }) {
            "Stage record already exists for stage=$stage"
        }

        stageRecords.add(
            HiringStageRecord(
                memberJobSummaryId = id,
                stage = stage,
                note = note
            )
        )
    }

    fun updateStage(stage: HiringStage, note: String, result: HiringStageResult?) {
        require(note.isNotBlank()) {
            "Stage note must not be blank"
        }
        val record = findStageRecord(stage)
        record.note = note
        record.result = result
        record.recordedAt = LocalDateTime.now()
    }

    fun removeStageRecord(stage: HiringStage) {
        stageRecords.removeIf { it.stage == stage }
    }

    /**
     * 조회 전용 (불변 컬렉션)
     */
    fun getStageRecords(): List<HiringStageRecord> =
        stageRecords.toList()

    private fun findStageRecord(stage: HiringStage): HiringStageRecord {
        return stageRecords.firstOrNull { it.stage == stage }
            ?: throw IllegalStateException("Stage record not found for stage=$stage")
    }
}
