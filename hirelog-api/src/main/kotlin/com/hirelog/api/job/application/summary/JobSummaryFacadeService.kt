package com.hirelog.api.job.application.summary.facade

import com.hirelog.api.brand.application.facade.BrandFacadeService
import com.hirelog.api.brand.domain.BrandSource
import com.hirelog.api.common.logging.log
import com.hirelog.api.common.utils.Normalizer
import com.hirelog.api.job.application.snapshot.facade.JobSnapshotFacadeService
import com.hirelog.api.job.application.summary.command.JobSummaryWriteService
import com.hirelog.api.job.application.summary.port.JobSummaryLlm
import com.hirelog.api.job.domain.JobSourceType
import com.hirelog.api.position.application.facade.PositionFacadeService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * JobSummary Facade Service
 *
 * 책임:
 * - JD 요약 유스케이스 오케스트레이션
 * - Brand / Position / Snapshot / Summary 흐름 조합
 */
@Service
class JobSummaryFacadeService(
    private val brandFacadeService: BrandFacadeService,
    private val positionFacadeService: PositionFacadeService,
    private val snapshotFacadeService: JobSnapshotFacadeService,
    private val jobSummaryWriteService: JobSummaryWriteService,
    private val summaryLlm: JobSummaryLlm,
) {

    /**
     * 텍스트 기반 JD 요약 생성
     *
     * 트랜잭션
     * - 외부 LLM 호출 포함
     */
    @Transactional
    fun summarizeTextAndSave(
        brandName: String,
        positionName: String,
        rawText: String
    ) {

        // 1️⃣ Brand 확보
        val normalizedBrand = Normalizer.normalizeBrand(brandName)

        val brand = brandFacadeService.getOrCreate(
            name = brandName,
            normalizedName = normalizedBrand,
            companyId = null,
            source = BrandSource.USER
        )

        // 2️⃣ Position 확보
        val normalizedPosition = Normalizer.normalizePosition(positionName)

        val position = positionFacadeService.getOrCreate(
            name = positionName,
            normalizedName = normalizedPosition,
            description = null
        )

        val snapshot = snapshotFacadeService.createIfNotExists(
            brandId = brand.id,
            positionId = position.id,
            rawText = rawText,
            sourceUrl = null,
            sourceType = JobSourceType.TEXT
        )

        val llmResult = try {
            summaryLlm.summarizeJobDescription(
                brandName = brand.name,
                position = position.name,
                jdText = rawText
            )
        } catch (e: Exception) {
            log.error(
                "[JobSummary] LLM call failed | snapshotId={}, message={}",
                snapshot.id,
                e.message,
                e
            )
            throw e
        }

        // 3️⃣ JobSummary 저장
        jobSummaryWriteService.save(
            snapshot = snapshot,
            brand = brand,
            position = position,
            llmResult = llmResult
        )

    }
}
