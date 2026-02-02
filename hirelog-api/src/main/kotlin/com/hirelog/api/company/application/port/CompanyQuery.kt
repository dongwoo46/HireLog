package com.hirelog.api.company.application.port

import com.hirelog.api.company.domain.Company

/**
 * Company Query Port
 *
 * 책임:
 * - Company 조회 전용 인터페이스
 * - 쓰기 로직 절대 포함 금지
 */
interface CompanyQuery {

    /**
     * ID 기준 단일 조회
     */
    fun findById(id: Long): Company?

    /**
     * 정규화된 이름 기준 조회
     */
    fun findByNormalizedName(normalizedName: String): Company?

    /**
     * 전체 회사명 조회 (LLM 후보 매칭용)
     */
    fun findAllNames(): List<String>
}
