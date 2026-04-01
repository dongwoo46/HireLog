package com.hirelog.api.job.presentation.controller.dto.request

import com.hirelog.api.common.exception.BusinessErrorCode
import com.hirelog.api.common.exception.BusinessException
import com.hirelog.api.job.domain.type.HiringStage
import com.hirelog.api.job.domain.type.ReviewSortType
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import java.time.LocalDate

data class JobSummaryReviewSearchReq(
    val jobSummaryId: Long? = null,
    val memberName: String? = null,
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
    val size: Int = 20,
    val sortBy: ReviewSortType = ReviewSortType.LATEST,
    val createdFrom: LocalDate? = null,
    val createdTo: LocalDate? = null,
    val includeDeleted: Boolean = false
) {
    fun validate() {
        if (minDifficultyRating != null && maxDifficultyRating != null) {
            if (minDifficultyRating > maxDifficultyRating) {
                throw BusinessException(
                    errorCode = BusinessErrorCode.INVALID_REVIEW_FILTER,
                    message = "난이도 최소값은 최대값보다 클 수 없습니다."
                )
            }
        }

        if (minSatisfactionRating != null && maxSatisfactionRating != null) {
            if (minSatisfactionRating > maxSatisfactionRating) {
                throw BusinessException(
                    errorCode = BusinessErrorCode.INVALID_REVIEW_FILTER,
                    message = "만족도 최소값은 최대값보다 클 수 없습니다."
                )
            }
        }

        if (createdFrom != null && createdTo != null) {
            if (createdFrom.isAfter(createdTo)) {
                throw BusinessException(
                    errorCode = BusinessErrorCode.INVALID_REVIEW_FILTER,
                    message = "생성 시작일은 종료일보다 이후일 수 없습니다."
                )
            }
        }
    }
}
