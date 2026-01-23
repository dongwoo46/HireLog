package com.hirelog.api.job.application.summary

import com.hirelog.api.job.application.snapshot.port.JobSnapshotQuery
import com.hirelog.api.job.application.summary.port.JobSummaryQuery
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.time.LocalDate

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
    private val summaryQuery: JobSummaryQuery
) {

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
        canonicalText: String,
    ): String {

        // 1. 최소한의 정규화만 수행 (환경 차이 제거 목적)
        val source = canonicalText
            .trim()
            .replace("\r\n", "\n")   // OS 개행 차이 제거
            .replace("\r", "\n")

        // 2. 해시 계산
        return sha256(source)
    }
    /**
     * JD 자체 중복 판정
     *
     * 기준:
     * - canonicalHash 기준
     *
     * 의미:
     * - 동일 JD는 요약 파이프라인에 재진입하지 않는다
     * - Snapshot은 이미 기록되었을 수 있다
     */
    fun isDuplicateSnapshot(contentHash: String): Boolean {
        return snapshotQuery.getSnapshotByContentHash(contentHash)!=null
    }

    /**
     * 요약 결과 중복 판정
     *
     * 기준:
     * - summaryText 자체 기준
     *
     * 목적:
     * - 동일 요약 결과 중복 저장 방지
     * - LLM 비용/데이터 낭비 방지
     */
//    fun isDuplicateSummary(summaryText: String): Boolean {
//        val summaryHash = sha256(summaryText.trim())
//        return summaryQuery.existsBySummaryHash(summaryHash)
//    }

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
}
