package com.hirelog.api.relation.domain.model

import com.hirelog.api.common.jpa.BaseEntity
import com.hirelog.api.relation.domain.type.MemberJobSummarySaveType
import jakarta.persistence.*

@Entity
@Table(
    name = "member_job_summary",
    indexes = [
        // 사용자가 저장한 JD 목록 조회
        Index(
            name = "idx_member_job_summary_member",
            columnList = "member_id"
        ),
        // 특정 JD를 저장한 사용자 조회 (분석용)
        Index(
            name = "idx_member_job_summary_job_summary",
            columnList = "job_summary_id"
        ),
        // 중복 저장 방지
        Index(
            name = "ux_member_job_summary_member_job",
            columnList = "member_id, job_summary_id",
            unique = true
        )
    ]
)
class MemberJobSummary(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    /**
     * 사용자 ID
     */
    @Column(name = "member_id", nullable = false)
    val memberId: Long,

    /**
     * 저장한 JD 요약 ID
     */
    @Column(name = "job_summary_id", nullable = false)
    val jobSummaryId: Long,

    /**
     * 저장 목적
     *
     * FAVORITE : 즐겨찾기
     * APPLY    : 지원 예정
     * COMPARE  : 다른 JD와 비교
     * ARCHIVE  : 나중에 다시 보기
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "save_type", nullable = false, length = 20)
    var saveType: MemberJobSummarySaveType = MemberJobSummarySaveType.FAVORITE,

    /**
     * 사용자 메모
     * (지원 전략, 느낌, 비교 포인트 등)
     */
    @Column(name = "memo", length = 2000)
    var memo: String? = null

) : BaseEntity() {

    companion object {

        /**
         * MemberJobSummary 생성
         *
         * 역할:
         * - 사용자가 JD 요약을 저장
         * - 기본 목적은 FAVORITE
         */
        fun create(
            memberId: Long,
            jobSummaryId: Long,
            saveType: MemberJobSummarySaveType = MemberJobSummarySaveType.FAVORITE,
            memo: String? = null
        ): MemberJobSummary {
            return MemberJobSummary(
                memberId = memberId,
                jobSummaryId = jobSummaryId,
                saveType = saveType,
                memo = memo
            )
        }
    }

    /**
     * 저장 목적 변경
     *
     * 역할:
     * - FAVORITE → APPLY / COMPARE / ARCHIVE 등
     */
    fun changeSaveType(newType: MemberJobSummarySaveType) {
        if (saveType == newType) return
        saveType = newType
    }

    /**
     * 메모 수정
     *
     * 역할:
     * - 사용자의 개인 메모 업데이트
     */
    fun updateMemo(newMemo: String?) {
        memo = newMemo
    }
}
