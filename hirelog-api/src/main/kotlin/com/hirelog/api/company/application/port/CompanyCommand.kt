package com.hirelog.api.company.application.port

import com.hirelog.api.company.domain.Company

/**
 * CompanyCommand
 *
 * 책임:
 * - Company Aggregate 쓰기 전용 포트
 * - 내부 write-flow에서 필요한 보조 조회 허용
 *
 * 주의:
 * - Controller / ReadService에서 사용 금지
 */
interface CompanyCommand {

    /**
     * 신규 / 변경 저장
     */
    fun save(company: Company): Company

    /**
     * 상태 변경 목적 단건 조회
     *
     * 특징:
     * - 상태 변경 전용
     * - 필요 시 비관적 락 적용
     */
    fun findById(id: Long): Company?

    /**
     * 쓰기 플로우 전용 조회
     *
     * 용도:
     * - Candidate → Company 변환 시 중복 생성 방지
     * - Scheduler / Batch 내부 전용
     *
     * 주의:
     * - 일반 조회 유스케이스에서 사용 ❌
     */
    fun findByNormalizedName(normalizedName: String): Company?
}
