package com.hirelog.api.job.presentation.controller.dto.request

import com.hirelog.api.job.domain.type.HiringStage
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

data class JobSummaryReviewSearchReq(

    val hiringStage: HiringStage? = null,

    @field:Min(1)
    @field:Max(10)
    val minDifficultyRating: Int? = null,

    @field:Min(1)
    @field:Max(10)
    val maxDifficultyRating: Int? = null,

    @field:Min(1)
    @field:Max(10)
    val minSatisfactionRating: Int? = null,

    @field:Min(1)
    @field:Max(10)
    val maxSatisfactionRating: Int? = null,

    @field:Min(0)
    val page: Int = 0,

    @field:Min(1)
    val size: Int = 20
) {

    /**
     * 범위 조건 정합성 검증
     *
     * - min <= max
     */
    fun validate() {
        if (minDifficultyRating != null && maxDifficultyRating != null) {
            require(minDifficultyRating <= maxDifficultyRating) {
                "난이도 최소값은 최대값보다 클 수 없습니다."
            }
        }

        if (minSatisfactionRating != null && maxSatisfactionRating != null) {
            require(minSatisfactionRating <= maxSatisfactionRating) {
                "만족도 최소값은 최대값보다 클 수 없습니다."
            }
        }
    }
}
