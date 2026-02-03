package com.hirelog.api.company.application.port

import com.hirelog.api.company.domain.Company

/**
 * CompanyCommand
 *
 * 책임:
 * - Company Aggregate 쓰기 전용 포트
 */
interface CompanyCommand {

    /**
     * 신규 / 변경 저장
     */
    fun save(company: Company):Company

    /**
     * 수정 목적 단건 조회
     *
     * 특징:
     * - 비관적 락
     * - verify / deactivate 등 상태 변경 전용
     */
    fun findById(id: Long): Company?

    /**
     * 중복 검증 / 동시성 대응용 조회
     */
    fun findByNormalizedName(normalizedName: String): Company?

}
