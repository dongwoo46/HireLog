package com.hirelog.api.job.application.summary.facade

import com.hirelog.api.brand.application.command.BrandWriteService
import com.hirelog.api.brand.domain.BrandSource
import com.hirelog.api.common.logging.log
import com.hirelog.api.common.utils.Normalizer
import com.hirelog.api.job.application.preprocess.JdPreprocessRequestService
import com.hirelog.api.job.application.summary.JobSummaryWriteService
import com.hirelog.api.job.application.summary.port.JobSummaryLlm
import com.hirelog.api.job.domain.JobSourceType
import com.hirelog.api.position.application.facade.PositionFacadeService
import org.springframework.stereotype.Service

/**
 * JobSummary Facade Service
 *
 * 책임:
 * - JD 요약 유스케이스 오케스트레이션
 * - Brand / Position / Snapshot / Summary 흐름 조합
 */
@Service
class JobSummaryFacadeService(
    private val brandWriteService: BrandWriteService,
    private val positionFacadeService: PositionFacadeService,
    private val jdPreprocessRequestService: JdPreprocessRequestService,
//    private val snapshotFacadeService: JobSnapshotFacadeService,
    private val jobSummaryWriteService: JobSummaryWriteService,
    private val summaryLlm: JobSummaryLlm,
) {

    /**
     * JD 요약 요청
     *
     * 처리 방식:
     * - 비동기 파이프라인 시작
     * - 즉시 반환
     */
    fun requestSummary(
        brandName: String,
        positionName: String,
        rawText: String
    ) {
        jdPreprocessRequestService.request(
            brandName = brandName,
            positionHint = positionName,
            rawText = rawText
        )
    }
}
