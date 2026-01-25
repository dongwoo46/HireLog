package com.hirelog.api.job.application.snapshot.command

import com.hirelog.api.job.domain.JobSourceType
import com.hirelog.api.job.domain.RecruitmentPeriodType
import jakarta.validation.constraints.NotNull
import java.time.LocalDate

/**
 * JobSnapshot 생성 Command
 *
 * 책임:
 * - 전처리 및 정책 판단이 완료된 JD Snapshot 입력값 전달
 *
 * 설계 원칙:
 * - 계산 ❌
 * - 중복 판정 ❌
 * - Domain 생성에 필요한 값만 전달
 *
 * 특징:
 * - canonicalMap은 그대로 저장 (JSONB)
 * - canonicalHash / simHash는 Application(Policy) 계층에서 계산 후 전달
 */
data class JobSnapshotCreateCommand(

    /**
     * JD 입력 소스 유형
     *
     * TEXT / OCR / URL
     */
    @field:NotNull
    val sourceType: JobSourceType,

    /**
     * URL 입력일 경우 원본 URL
     */
    val sourceUrl: String?,

    /**
     * 전처리된 JD 섹션 구조
     *
     * 예:
     * - responsibilities
     * - requirements
     * - preferred
     * - techStack
     *
     * 저장 목적:
     * - 중복 판정 재계산
     * - JD 변경 추적
     */
    @field:NotNull
    val canonicalMap: Map<String, List<String>>,

    /**
     * pg_trgm 비교용 핵심 텍스트
     *
     * 구성 책임:
     * - responsibilities
     * - requirements
     * - preferred
     * - process (low weight)
     *
     * 주의:
     * - 파생 데이터
     * - Application 계층에서 생성
     */
    @field:NotNull
    val coreText: String,

    /**
     * 채용 기간 유형
     *
     * FIXED / OPEN_ENDED / UNKNOWN
     */
    @field:NotNull
    val recruitmentPeriodType: RecruitmentPeriodType,

    /**
     * 채용 지원 시작일
     *
     * - null 가능
     */
    val openedDate: LocalDate?,

    /**
     * 채용 지원 마감일
     *
     * - null이면 상시채용 또는 미정
     */
    val closedDate: LocalDate?,

    /**
     * 완전 동일 JD 식별자
     *
     * 생성 기준:
     * - canonicalMap → deterministic flatten → SHA-256
     *
     * 용도:
     * - Fast-path 중복 제거
     * - DB unique constraint
     */
    @field:NotNull
    val canonicalHash: String,

    /**
     * 의미적 유사성 판정용 SimHash
     *
     * 생성 기준:
     * - canonicalMap → weighted tokenization → SimHash
     *
     * 용도:
     * - LLM 이전 의미적 중복 판정
     */
    @field:NotNull
    val simHash: Long
)
