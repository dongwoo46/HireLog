package com.hirelog.api.company.application

import com.hirelog.api.common.exception.EntityNotFoundException
import com.hirelog.api.company.application.port.CompanyCommand
import com.hirelog.api.company.application.port.CompanyQuery
import com.hirelog.api.company.domain.Company
import com.hirelog.api.company.domain.CompanySource
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Company Write Service
 *
 * 책임:
 * - Company 쓰기 유스케이스 전담
 * - 트랜잭션 경계 정의
 * - 중복 / 존재 정책 보장
 */
@Service
class CompanyWriteService(
    private val companyCommand: CompanyCommand,
    private val companyQuery: CompanyQuery
) {

    /**
     * Company 확보
     *
     * 정책:
     * - normalizedName 기준 단일 Company 보장
     * - 존재하면 반환
     * - 없으면 신규 생성
     */
    @Transactional
    fun getOrCreate(
        name: String,
        normalizedName: String,
        aliases: List<String>,
        source: CompanySource,
        externalId: String?
    ): Company {

        // 1. 빠른 조회 (UX / 의미용)
        companyQuery.findByNormalizedName(normalizedName)?.let {
            return it
        }

        val company = Company.create(
            name = name,
            aliases = aliases,
            source = source,
            externalId = externalId
        )

        // 2. DB가 최종 중복을 판단하도록 위임
        return try {
            companyCommand.save(company)
        } catch (ex: DataIntegrityViolationException) {
            // 3. 동시성으로 이미 생성된 경우 재조회
            companyQuery.findByNormalizedName(normalizedName)
                ?: throw ex
        }
    }

    /**
     * 회사 검증 승인
     */
    @Transactional
    fun verify(companyId: Long) {
        getRequired(companyId).verify()
    }

    /**
     * 회사 비활성화
     */
    @Transactional
    fun deactivate(companyId: Long) {
        getRequired(companyId).deactivate()
    }

    /**
     * 쓰기 유스케이스 전용 필수 조회
     */
    private fun getRequired(companyId: Long): Company =
        companyQuery.findById(companyId)
            ?: throw EntityNotFoundException(
                entityName = "Company",
                identifier = companyId
            )

}
