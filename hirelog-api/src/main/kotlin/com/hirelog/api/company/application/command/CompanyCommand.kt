package com.hirelog.api.company.application.command

import com.hirelog.api.company.domain.Company

/**
 * Company Write Port
 *
 * 책임:
 * - Company Aggregate 영속화 추상화
 *
 * 설계 원칙:
 * - 유스케이스 ❌
 * - 정책 ❌
 * - 저장 행위만 정의
 */
interface CompanyCommand {

    /**
     * Company 저장
     *
     * - 신규 / 수정 모두 포함
     */
    fun save(company: Company): Company
}
