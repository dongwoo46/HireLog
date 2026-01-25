package com.hirelog.api.brand.application.command

import com.hirelog.api.brand.domain.Brand
import com.hirelog.api.brand.domain.BrandSource
import com.hirelog.api.common.domain.VerificationStatus

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

    /**
     * Brand INSERT with ON CONFLICT DO NOTHING
     *
     * 동시성 안전한 Brand 생성:
     * - normalized_name 중복 시 아무 작업도 하지 않음
     * - 반환값: 영향 받은 row 수 (0 = 중복, 1 = 신규)
     */
    fun insertIgnoreDuplicate(
        name: String,
        normalizedName: String,
        companyId: Long?,
        verificationStatus: VerificationStatus,
        source: BrandSource,
        isActive: Boolean
    ): Int
}
