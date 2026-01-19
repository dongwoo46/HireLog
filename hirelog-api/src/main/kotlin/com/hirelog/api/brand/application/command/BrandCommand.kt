package com.hirelog.api.brand.application.command

import com.hirelog.api.brand.domain.Brand

/**
 * Brand Write Port
 *
 * 책임:
 * - Brand Aggregate 영속화 추상화
 *
 * 설계 원칙:
 * - 유스케이스 ❌
 * - 상태 변경 ❌
 * - 저장 행위만 표현
 */
interface BrandCommand {

    /**
     * Brand 저장
     *
     * - 신규/수정 모두 포함
     * - JPA Dirty Checking 기반
     */
    fun save(brand: Brand): Brand
}
