package com.hirelog.api.company.service

import com.hirelog.api.company.domain.Company
import com.hirelog.api.company.domain.CompanySource
import com.hirelog.api.company.repository.CompanyRepository
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service

@Service
class CompanyCommandService(
    private val companyRepository: CompanyRepository
) {

    /**
     * Company 생성
     *
     * 역할:
     * - 새로운 회사 엔티티를 생성한다
     *
     * 흐름:
     * 1) normalizedName 중복 검사
     * 2) 도메인 팩토리(Company.create)로 생성
     * 3) 저장
     */
    @Transactional
    fun create(
        name: String,
        normalizedName: String,
        aliases: List<String>,
        source: CompanySource,
        externalId: String?
    ): Company {
        
        // 데이터 검증
        require(!companyRepository.existsByNormalizedName(normalizedName)) {
            "Company already exists: $normalizedName"
        }

        val company = Company.create(
            name = name,
            normalizedName = normalizedName,
            aliases = aliases,
            source = source,
            externalId = externalId
        )

        return companyRepository.save(company)
    }

    /**
     * 회사 검증 승인
     *
     * 역할:
     * - 회사의 검증 상태를 VERIFIED로 전환
     *
     * 특징:
     * - idempotent (이미 VERIFIED면 변경 없음)
     */
    @Transactional
    fun verify(companyId: Long) {
        val company = getOrThrow(companyId)
        company.verify()
    }

    /**
     * 회사 비활성화
     *
     * 역할:
     * - 회사를 논리적으로 비활성화
     * - 실제 삭제는 하지 않음
     */
    @Transactional
    fun deactivate(companyId: Long) {
        val company = getOrThrow(companyId)
        company.deactivate()
    }

    /**
     * 내부 공통 조회 함수
     *
     * 역할:
     * - Company 조회
     * - 없으면 즉시 예외 발생
     */
    private fun getOrThrow(companyId: Long): Company =
        companyRepository.findById(companyId)
            .orElseThrow { IllegalArgumentException("Company not found: $companyId") }
}
