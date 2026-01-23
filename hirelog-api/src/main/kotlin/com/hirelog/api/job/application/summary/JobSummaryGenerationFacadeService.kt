package com.hirelog.api.job.application.summary.facade

import com.hirelog.api.brand.application.command.BrandWriteService
import com.hirelog.api.brandposition.application.BrandPositionWriteService
import com.hirelog.api.job.application.jobsummaryprocessing.JdSummaryProcessingWriteService
import com.hirelog.api.job.application.messaging.JobSummaryPreprocessResponseMessage
import com.hirelog.api.job.application.snapshot.JobSnapshotWriteService
import com.hirelog.api.job.application.snapshot.command.JobSnapshotCreateCommand
import com.hirelog.api.job.application.snapshot.port.JobSnapshotQuery
import com.hirelog.api.job.application.summary.JdIntakePolicy
import com.hirelog.api.job.application.summary.JobSummaryWriteService
import com.hirelog.api.job.application.summary.port.JobSummaryLlm
import com.hirelog.api.job.domain.JobSnapshot
import com.hirelog.api.job.domain.JobSourceType
import org.springframework.stereotype.Service

/**
 * JobSummaryGenerationFacadeService
 *
 * 책임:
 * - JD 요약 파이프라인 전체 흐름 오케스트레이션
 *
 * 설계 원칙:
 * - Facade는 "순서 + 분기 + 상태 전이 호출"만 담당
 * - 실제 저장 / 상태 변경은 각각의 WriteService로 위임
 */
@Service
class JobSummaryGenerationFacadeService(
    private val processingWriteService: JdSummaryProcessingWriteService,
    private val snapshotWriteService: JobSnapshotWriteService,
    private val jdIntakePolicy: JdIntakePolicy,
    private val llmClient: JobSummaryLlm,
    private val summaryWriteService: JobSummaryWriteService,
    private val brandWriteService: BrandWriteService,
    private val brandPositionWriteService: BrandPositionWriteService,
    private val snapshotQuery: JobSnapshotQuery,

    ) {

    /**
     * 전처리 결과 기반 JD 요약 파이프라인 진입점
     */
    fun generateFromPreprocessResult(
        message: JobSummaryPreprocessResponseMessage
    ) {

        /**
         * 0️⃣ JobSummaryProcessing 생성 (항상 최초)
         *
         * 이유:
         * - 모든 요청은 반드시 처리 이력을 가져야 함
         * - Snapshot만 저장되고 추적 불가능해지는 상황 방지
         */
        val processing = processingWriteService.startProcessing(
            requestId = message.requestId
        )

        try {

            /**
             *  1. canonicalHash 계산 (정책 책임)
             *
             * message.canonicalText 기반 hash로 매핑
             */
            val contentHash =
                jdIntakePolicy.calculateCanonicalHash(
                    canonicalText = message.canonicalText,
                )

             /**
             * 2. JD 자체 중복 판정 (LLM 이전, Fast-Path)
             * snapshot에 contentHash 중복 체크 후 바로 제외
             *
             * 결과:
             * - 중복이면 Processing만 DUPLICATE로 종료
             * - Snapshot은 이미 저장됨
             */
            if (snapshotQuery.getSnapshotByContentHash(contentHash)!=null) {
                processingWriteService.markDuplicate(
                    processingId = processing.id,
                    reason = "HASH_DUPLICATE"
                )
                return
            }

            /**
             * 3. Snapshot 기록 (무조건 수행)
             * - 수집 로그 성격
             * - 중복 여부와 무관
             */
            val snapshotId = snapshotWriteService.record(
                JobSnapshotCreateCommand(
                    sourceType = message.source,
                    sourceUrl = message.sourceUrl,
                    rawText = message.canonicalText,
                    contentHash = contentHash,
                    openedDate = message.openedDate,
                    closedDate = message.closedDate
              )
            )

            /**
             *   4-1. message.source가 url일때 sourceUrl인 snapshot 의심후보
             *   4-2. message.cononicalText에 snapshot의 공고 시작 날짜 끝나는 날짜로 필터링하여 의심후보 등록
             *   4-3. 의심후보의 각 필드 요소들(핵심업무,필수역량,기술스택,우대사항)과 message.cononicalText의 유사도 체크 후 일정 수치 이상이면 중복으로 판정
             *   4-4. 의심후보가 없거나 유사도 일정 수치 이하일때 통과
             */
            var suspectSnapshots: List<JobSnapshot> = emptyList()

            if (message.source == JobSourceType.URL && message.sourceUrl != null) {
                suspectSnapshots = snapshotQuery.loadSnapshotsByUrl(message.sourceUrl)
            }

            // 2. 날짜 기준 의심 후보 (openedDate 또는 closedDate 중 하나라도 있으면)
            if (message.openedDate != null || message.closedDate != null) {
                suspectSnapshots = suspectSnapshots + snapshotQuery.loadSnapshotsByDateRange(
                    openedDate = message.openedDate,
                    closedDate = message.closedDate
                )
            }

            /**
             * 5. 요약 단계 진입 상태 기록
             */
            processingWriteService.markSummarizing(processing.id)

            /**
             * 6. llm 요청을 통해 summarize 시도
             *
             * llm호출 비동기 (트랜잭션x)
             * * 결과:
             * - 브랜드명 추정
             * - 포지션명 추정
             * - 요약 텍스트
             * - 경력/기술스택 등
             */
            val llmResult =
                llmClient.summarizeJobDescription(
                    brandName = message.brandName,
                    positionName = message.positionName,
                    canonicalText = message.canonicalText
                )

//
//            /**
//             * 6️⃣ 요약 결과 중복 판정 (의미적 중복)
//             *
//             * 채용공고 시간이 null 이라면 상시채용
//             * 요약결과  brandName, positonName 유사도 체크 후 일정 수치 이상이면 중복으로 판정
//             */
//            if (jdIntakePolicy.isDuplicateSummary(llmResult)) {
//                processingWriteService.markDuplicate(
//                    processingId = processing.id,
//                    reason = "SEMANTIC_DUPLICATE"
//                )
//                return
//            }
//
//            /**
//             * 7️⃣ Brand / Position / BrandPosition 도메인 생성
//             *
//             * 생성 실패시 예외 발생 processing FAILED 처리
//             *
//             * 책임:
//             * - 여러 Aggregate 조합
//             * - Facade 레벨에서만 수행
//             */
//            val brand =
//                brandWriteService.getOrCreate(llmResult.brand)
//
//            val position =
//                brandPositionWriteService.getOrCreate(
//                    brandId = brand.id,
//                    positionName = llmResult.position
//                )
//
//            val brandPosition = brandPositionWriteService.create()
//
//            /**
//             * 8️⃣ JobSummary 저장 (도메인 생성 책임)
//             *
//             * 조건:
//             * - 모든 중복 판정 통과
//             */
//            summaryWriteService.save(
//                snapshotId = snapshotId,
//                brand = brand,
//                position = position,
//                llmResult = llmResult
//            )
//
//            /**
//             * 9️⃣ Processing 완료 상태 기록
//             */
//            processingWriteService.markCompleted(processing.id)

        } catch (e: Exception) {
            /**
             * ❌ 실패 상태 기록
             *
             * 정책:
             * - Snapshot은 이미 저장됨
             * - Processing에 실패 원인 기록
             */
            processingWriteService.markFailed(processing.id, errorCode = "404", errorMessage = e.message ?: "Unknown error")
            throw e
        }
    }
}
