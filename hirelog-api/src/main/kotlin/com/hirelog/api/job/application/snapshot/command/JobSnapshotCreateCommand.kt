package com.hirelog.api.job.application.snapshot.command

import com.hirelog.api.job.domain.JobSourceType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDate

/**
 * JobSnapshot 생성 Command
 *
 * 특징:
 * - contentHash는 외부에서 받지 않는다
 * - rawText 기반으로 Domain에서 생성된다
 */
data class JobSnapshotCreateCommand(

    @field:NotNull
    val sourceType: JobSourceType,

    @field:Size(max = 1000)
    val sourceUrl: String?,

    @field:NotBlank
    val rawText: String,

    @field:NotBlank
    val contentHash: String,

    /**
     * 채용 지원 시작일
     *
     * - 시스템이 판단한 값
     */
    val openedDate: LocalDate?,

    /**
     * 채용 지원 마감일
     *
     * - null이면 상시채용
     */
    val closedDate: LocalDate?

)
