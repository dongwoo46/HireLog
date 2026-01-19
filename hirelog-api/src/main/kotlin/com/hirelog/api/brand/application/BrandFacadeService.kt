package com.hirelog.api.brand.application.facade

import com.hirelog.api.brand.application.command.BrandWriteService
import com.hirelog.api.brand.application.query.BrandQuery
import com.hirelog.api.brand.domain.Brand
import com.hirelog.api.brand.domain.BrandSource
import org.springframework.stereotype.Service

/**
 * Brand Facade Service
 *
 * 책임:
 * - Brand 관련 유스케이스의 단일 진입점
 * - 조회/생성 정책 결정
 * - Write / Query 조합 오케스트레이션
 *
 * 설계 원칙:
 * - BrandQuery는 절대 예외를 던지지 않는다
 * - "반드시 존재해야 하는 Brand" 판단은 Facade 책임
 */
@Service
class BrandFacadeService(
    private val brandQuery: BrandQuery,
    private val brandWriteService: BrandWriteService
) {

    /**
     * normalizedName 기준 Brand 확보
     *
     * 정책:
     * - 이미 존재하면 해당 Brand 반환
     * - 존재하지 않으면 신규 생성
     *
     * 사용 시나리오:
     * - JD 수집
     * - 외부 입력 기반 Brand 자동 등록
     */
    fun getOrCreate(
        name: String,
        normalizedName: String,
        companyId: Long?,
        source: BrandSource
    ): Brand {
        return brandQuery.findByNormalizedName(normalizedName)
            ?: brandWriteService.create(
                name = name,
                normalizedName = normalizedName,
                companyId = companyId,
                source = source
            )
    }

    /**
     * Brand 단건 조회 (필수)
     *
     * 정책:
     * - Brand는 반드시 존재해야 한다
     * - 없으면 즉시 예외 발생
     *
     * 사용 시나리오:
     * - 내부 참조
     * - 연관 도메인(Job, Position) 처리
     */
    fun getRequiredById(brandId: Long): Brand =
        requireNotNull(brandQuery.findById(brandId)) {
            "Brand not found: $brandId"
        }

    /**
     * Brand 단건 조회 (옵션)
     *
     * 정책:
     * - 존재하지 않을 수 있음
     * - 판단은 호출자에게 위임
     *
     * 사용 시나리오:
     * - 중복 체크
     * - 조건부 처리
     */
    fun findById(brandId: Long): Brand? =
        brandQuery.findById(brandId)

    /**
     * normalizedName 기준 Brand 조회
     *
     * 정책:
     * - 중복 판단 전용
     *
     * 주의:
     * - 생성 로직 ❌
     * - 조회만 수행
     */
    fun findByNormalizedName(normalizedName: String): Brand? =
        brandQuery.findByNormalizedName(normalizedName)
}
