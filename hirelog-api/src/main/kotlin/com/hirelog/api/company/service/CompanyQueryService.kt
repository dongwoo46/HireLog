package com.hirelog.api.company.service

import com.hirelog.api.company.domain.Company
import com.hirelog.api.company.repository.CompanyRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CompanyQueryService(
    private val companyRepository: CompanyRepository
) {

    /**
     * Company 단건 조회 (ID 기준)
     *
     * 역할:
     * - 내부 비즈니스 로직
     * - API 응답용 조회
     */
    @Transactional(readOnly = true)
    fun findById(companyId: Long): Company =
        companyRepository.findByIdOrNull(companyId)
            ?: throw IllegalArgumentException("Company not found: $companyId")

    /**
     * normalizedName 기준 조회
     *
     * 역할:
     * - 중복 검사
     * - 외부 입력(Brand, JD 등) 매핑
     */
    @Transactional(readOnly = true)
    fun findByNormalizedName(normalizedName: String): Company? =
        companyRepository.findByNormalizedName(normalizedName)
}
