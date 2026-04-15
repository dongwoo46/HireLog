package com.hirelog.api.job.presentation.controller.dto.request

import com.hirelog.api.job.application.summary.query.JobSummarySearchQuery
import com.hirelog.api.job.domain.type.CareerType

data class JobSummarySearchReq(
    val keyword: String? = null,
    val careerType: String? = null,
    val careerTypes: List<String>? = null,

    val brandId: Long? = null,
    val companyId: Long? = null,
    val positionId: Long? = null,
    val brandPositionId: Long? = null,
    val positionCategoryId: Long? = null,
    val brandIds: List<Long>? = null,
    val companyIds: List<Long>? = null,
    val positionIds: List<Long>? = null,
    val brandPositionIds: List<Long>? = null,
    val positionCategoryIds: List<Long>? = null,

    val brandName: String? = null,
    val positionName: String? = null,
    val brandPositionName: String? = null,
    val positionCategoryName: String? = null,
    val brandNames: List<String>? = null,
    val positionNames: List<String>? = null,
    val brandPositionNames: List<String>? = null,
    val positionCategoryNames: List<String>? = null,

    val techStacks: List<String>? = null,
    val companyDomains: List<String>? = null,
    val companySizes: List<String>? = null,
    val cursor: String? = null,
    val size: Int = 20,
    val sortBy: String = "CREATED_AT_DESC"
) {
    private fun <T> merge(single: T?, multiple: List<T>?): List<T>? {
        val merged = buildList {
            if (single != null) add(single)
            multiple?.forEach { add(it) }
        }.distinct()
        return merged.takeIf { it.isNotEmpty() }
    }

    private fun normalizeStrings(values: List<String>?): List<String>? {
        return values
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.distinct()
            ?.takeIf { it.isNotEmpty() }
    }

    fun toQuery(): JobSummarySearchQuery {
        val mergedCareerTypes = merge(careerType, careerTypes)
            ?.mapNotNull {
                if (it.equals("ANY", ignoreCase = true)) null
                else runCatching { CareerType.valueOf(it) }.getOrNull()
            }
            ?.distinct()
            ?.takeIf { it.isNotEmpty() }

        return JobSummarySearchQuery(
            keyword = keyword?.takeIf { it.isNotBlank() },
            careerTypes = mergedCareerTypes,
            brandIds = merge(brandId, brandIds),
            companyIds = merge(companyId, companyIds),
            positionIds = merge(positionId, positionIds),
            brandPositionIds = merge(brandPositionId, brandPositionIds),
            positionCategoryIds = merge(positionCategoryId, positionCategoryIds),
            brandNames = normalizeStrings(merge(brandName, brandNames)),
            positionNames = normalizeStrings(merge(positionName, positionNames)),
            brandPositionNames = normalizeStrings(merge(brandPositionName, brandPositionNames)),
            positionCategoryNames = normalizeStrings(merge(positionCategoryName, positionCategoryNames)),
            techStacks = normalizeStrings(techStacks),
            companyDomains = normalizeStrings(companyDomains),
            companySizes = normalizeStrings(companySizes),
            cursor = cursor,
            size = size.coerceIn(1, 100),
            sortBy = runCatching {
                JobSummarySearchQuery.SortBy.valueOf(sortBy)
            }.getOrDefault(JobSummarySearchQuery.SortBy.CREATED_AT_DESC)
        )
    }
}
