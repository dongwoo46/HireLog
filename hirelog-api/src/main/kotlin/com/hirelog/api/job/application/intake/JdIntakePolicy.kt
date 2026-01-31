package com.hirelog.api.job.application.intake

import com.hirelog.api.common.infra.storage.FileStorageService
import com.hirelog.api.job.application.intake.model.DuplicateDecision
import com.hirelog.api.job.application.intake.model.IntakeHashes
import com.hirelog.api.job.application.intake.model.JdIntakeInput
import com.hirelog.api.job.application.intake.port.JdPreprocessRequestPort
import com.hirelog.api.job.application.messaging.JdPreprocessRequestMessage
import com.hirelog.api.job.application.snapshot.port.JobSnapshotQuery
import com.hirelog.api.job.application.summary.port.JobSummaryQuery
import com.hirelog.api.job.domain.JobSnapshot
import com.hirelog.api.job.domain.JobSourceType
import com.hirelog.api.job.intake.similarity.SimHashCalculator
import com.hirelog.api.job.intake.similarity.SimHashSimilarity
import org.springframework.stereotype.Service
import java.security.MessageDigest
import com.hirelog.api.job.application.summary.command.JobSummaryGenerateCommand
import org.springframework.web.multipart.MultipartFile
import java.util.*

/**
 * JdIntakePolicy
 *
 * 역할:
 * - JD 유입 정책 결정자
 * - 파이프라인 진입 여부 판단
 *
 * 책임:
 * - canonicalHash 생성
 * - JD 중복 판정
 * - 요약 결과 중복 판정
 *
 * 비책임:
 * - DB 저장 ❌
 * - 상태 변경 ❌
 * - 도메인 생성 ❌
 */
@Service
class JdIntakePolicy(
    private val snapshotQuery: JobSnapshotQuery,
    private val summaryQuery: JobSummaryQuery,
    private val fileStorageService: FileStorageService,
    private val jdPreprocessRequestPort: JdPreprocessRequestPort,
) {

    /**
     * TEXT 기반 JD 전처리 요청
     */
    fun requestText(
        brandName: String,
        positionName: String,
        text: String,
    ): String {
        return send(
            brandName = brandName,
            positionName = positionName,
            source = JobSourceType.TEXT,
            payload = text,
        )
    }

    /**
     * OCR 기반 JD 전처리 요청
     */
    fun requestOcr(
        brandName: String,
        positionName: String,
        imageFiles: List<MultipartFile>,
    ): String {
        val savedPaths = fileStorageService.saveImages(imageFiles, "ocr")

        return send(
            brandName = brandName,
            positionName = positionName,
            source = JobSourceType.IMAGE,
            payload = savedPaths.joinToString(","),
        )
    }

    /**
     * URL 기반 JD 전처리 요청
     */
    fun requestUrl(
        brandName: String,
        positionName: String,
        url: String,
    ): String {
        require(isValidUrl(url)) { "Invalid URL format: $url" }

        return send(
            brandName = brandName,
            positionName = positionName,
            source = JobSourceType.URL,
            payload = url,
        )
    }

    /**
     * 공통 전처리 요청 로직
     */
    private fun send(
        brandName: String,
        positionName: String,
        source: JobSourceType,
        payload: String,
    ): String {
        require(brandName.isNotBlank())
        require(positionName.isNotBlank())
        require(payload.isNotBlank())

        val message = JdPreprocessRequestMessage(
            eventId = UUID.randomUUID().toString(),
            requestId = UUID.randomUUID().toString(),
            occurredAt = System.currentTimeMillis(),
            version = "v1",
            brandName = brandName,
            positionName = positionName,
            source = source,
            text = payload,
        )

        jdPreprocessRequestPort.send(message)

        return message.requestId
    }

    private fun isValidUrl(url: String): Boolean {
        return try {
            val uri = java.net.URI(url)
            uri.scheme in listOf("http", "https") && uri.host != null
        } catch (e: Exception) {
            false
        }
    }

    fun generateIntakeHashes(
        canonicalMap: Map<String, List<String>>
    ): IntakeHashes {

        val canonicalHash = calculateCanonicalHash(canonicalMap)
        val simHash = SimHashCalculator.calculate(canonicalMap)
        val coreText = buildCoreText(canonicalMap)

        return IntakeHashes(
            canonicalHash = canonicalHash,
            simHash = simHash,
            coreText = coreText
        )
    }

    fun decideDuplicate(
        command: JobSummaryGenerateCommand,
        hashes: IntakeHashes
    ): DuplicateDecision {

        // 1️⃣ 완전 동일 중복 (Fast-path)
        if (isHashDuplicate(hashes.canonicalHash)) {
            return DuplicateDecision.HASH_DUPLICATE
        }

        // 2️⃣ 의심 후보 수집
        val suspects = collectSuspectSnapshots(command)
        if (suspects.isEmpty()) {
            return DuplicateDecision.NOT_DUPLICATE
        }

        // 3️⃣ SimHash (1차 의미적 중복)
        if (isSimHashDuplicate(suspects, hashes.simHash)) {
            return DuplicateDecision.SIMHASH_DUPLICATE
        }

        // 4️⃣ pg_trgm (최종 의미적 중복)
        if (isTrgmDuplicate(hashes.coreText)) {
            return DuplicateDecision.TRGM_DUPLICATE
        }

        return DuplicateDecision.NOT_DUPLICATE
    }



    /**
     * coreText 생성 정책
     *
     * 책임:
     * - JD 요약 중복 판정용 핵심 텍스트 생성
     *
     * 설계 이유:
     * - pg_trgm 유사도 비교 시
     *   불필요한 노이즈 제거 목적
     * - process 섹션은 낮은 가중치로 취급
     */
    fun buildCoreText(sections: Map<String, List<String>>): String {
        return buildString {
            sections["responsibilities"]?.forEach { appendLine(it) }
            sections["requirements"]?.forEach { appendLine(it) }
            sections["preferred"]?.forEach { appendLine(it) }
            sections["process"]?.forEach { appendLine(it) } // low weight
        }.trim()
    }

    /**
     * JD 식별용 canonicalHash 생성
     *
     * 기준:
     * - canonicalText (정규화된 JD)
     * - openedDate / closedDate
     *
     * 설계 이유:
     * - 날짜가 다르면 다른 공고로 취급
     * - 숫자/포맷 노이즈 제거는 전처리 단계 책임
     */
    fun calculateCanonicalHash(
        canonicalMap: Map<String, List<String>>
    ): String {

        val normalized = canonicalMap
            .toSortedMap() // key order 고정
            .entries
            .joinToString("\n") { (key, lines) ->
                buildString {
                    append(key).append(":\n")
                    lines.forEach { append(it).append("\n") }
                }
            }
            .trim()
            .replace("\r\n", "\n")
            .replace("\r", "\n")

        return sha256(normalized)
    }


    /**
     * 중복 판정을 위한 Snapshot 의심 후보 수집
     *
     * 책임:
     * - 현재 JD와 "같은 공고일 가능성이 있는 Snapshot"만 선별
     * - 전체 Snapshot 스캔 방지
     *
     * 수집 기준:
     * 1. URL 기준
     *    - source가 URL인 경우
     *    - 동일 URL에서 수집된 Snapshot은
     *      동일 회사/동일 공고일 가능성이 매우 높음
     *
     * 2. 날짜 기준
     *    - openedDate 또는 closedDate 중 하나라도 존재하는 경우
     *    - 같은 채용 기간을 가진 Snapshot을 의심 후보로 간주
     *
     * 주의:
     * - 이 메서드는 "중복 여부를 판단하지 않는다"
     * - 오직 의심 후보 수집만 담당
     */
    private fun collectSuspectSnapshots(
        command: JobSummaryGenerateCommand
    ): List<JobSnapshot> {

        val result = mutableSetOf<JobSnapshot>()

        // 4-1. URL 기준 의심 후보
        if (command.source == JobSourceType.URL && command.sourceUrl != null) {
            result += snapshotQuery.loadSnapshotsByUrl(command.sourceUrl)
        }

        // 4-2. 날짜 기준 의심 후보
        if (command.openedDate != null || command.closedDate != null) {
            result += snapshotQuery.loadSnapshotsByDateRange(
                openedDate = command.openedDate,
                closedDate = command.closedDate
            )
        }

        return result.toList()
    }

    /**
     * JD 입력 자체의 유효성 판정
     *
     * 책임:
     * - 이 JD가 "분석할 가치가 있는 입력인지"만 판단
     *
     * 판단 기준 예시:
     * - canonicalText 길이 최소 기준 충족
     * - 필수 섹션(업무/자격요건 등) 존재 여부
     * - 노이즈 텍스트(OCR 실패 등) 여부
     *
     * 결과:
     * - false면 요약/중복/저장 파이프라인 진입 ❌
     * - true면 다음 단계 진행
     */
    fun isValidJd(command: JobSummaryGenerateCommand): Boolean {

        val canonicalText = flattenCanonicalText(command.canonicalMap)

        if (canonicalText.isBlank()) return false
        if (canonicalText.length < 300) return false

        // 필수 섹션 최소 요건
        if (command.canonicalMap["responsibilities"].isNullOrEmpty()) return false
        if (command.canonicalMap["requirements"].isNullOrEmpty()) return false
        if (command.canonicalMap["preferred"].isNullOrEmpty()) return false

        return true
    }

    private fun isHashDuplicate(canonicalHash: String): Boolean {
        return snapshotQuery.getSnapshotByCanonicalHash(canonicalHash) != null
    }

    /**
     * JD 중복 판정
     *
     * 단계:
     * 1. 완전 동일 중복 (contentHash)
     * 2. SimHash 기반 의미적 중복 (1차)
     *
     * 의미:
     * - true면 신규 JD 아님
     * - false면 신규 JD
     */
    private fun isSimHashDuplicate(
        suspects: List<JobSnapshot>,
        inputSimHash: Long
    ): Boolean {
        return suspects.any { snapshot ->
            snapshot.simHash != null &&
                    SimHashSimilarity.isDuplicate(
                        a = inputSimHash,
                        b = snapshot.simHash
                    )
        }
    }

    /**
     * pg_trgm 기반 텍스트 유사도 중복 판정
     *
     * 의미:
     * - SimHash 이후 최종 중복 판정
     * - coreText 기준
     */
    private fun isTrgmDuplicate(
        coreText: String,
        threshold: Double = 0.75
    ): Boolean {
        if (coreText.isBlank()) return false

        return snapshotQuery
            .findSimilarByCoreText(coreText, threshold)
            .isNotEmpty()
    }




    /**
     * JobSummaryPreprocessResponseMessage → JdIntakeInput 변환
     *
     * 책임:
     * - 전처리 결과(canonicalMap)를
     *   Intake 단계에서 사용할 입력 모델로 변환
     * - 이 단계에서 정책적으로 canonicalText를 생성한다
     *
     * 주의:
     * - 이 함수는 "가공"은 허용하지만
     *   "도메인 규칙"은 포함하지 않는다
     */
    fun toIntakeInput(
        command: JobSummaryGenerateCommand
    ): JdIntakeInput {

        val map = command.canonicalMap

        return JdIntakeInput(

            requiredTexts = map["requirements"].orEmpty(),
            responsibilityTexts = map["responsibilities"].orEmpty(),
            preferredTexts = map["preferred"].orEmpty(),

            openedDate = command.openedDate,
            closedDate = command.closedDate,
            recruitmentType = command.recruitmentPeriodType,

            source = command.source,
            sourceUrl = command.sourceUrl,

            skills = command.skills,
            process = map["process"].orEmpty()
        )
    }

    /**
     * SHA-256 해시 유틸
     *
     * 주의:
     * - 반드시 UTF-8 고정
     * - 소문자 hex로 통일
     */
    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    // 섹션 구조를 잃고, 하나의 연속된 canonical text로 변환
    private fun flattenCanonicalText(
        canonicalMap: Map<String, List<String>>
    ): String {
        return canonicalMap.values
            .flatten()
            .joinToString("\n")
    }

}
