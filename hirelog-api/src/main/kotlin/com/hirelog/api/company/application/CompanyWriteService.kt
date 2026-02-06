package com.hirelog.api.company.application

import com.hirelog.api.common.config.security.AuthenticatedMember
import com.hirelog.api.common.exception.EntityNotFoundException
import com.hirelog.api.common.utils.Normalizer
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
 * - 관리자 권한 최종 검증
 *
 * 설계 원칙:
 * - 인증 주체는 명시적으로 전달받는다
 * - 권한 판단은 반드시 Application Layer에서 수행한다
 */
@Service
class CompanyWriteService(
    private val companyCommand: CompanyCommand,
    private val companyQuery: CompanyQuery
) {

    /**
     * Company 생성
     *
     * 정책:
     * - 관리자만 생성 가능
     * - normalizedName 중복 불가
     */
    @Transactional
    fun create(
        name: String,
        source: CompanySource,
        externalId: String?,
        member: AuthenticatedMember
    ): Long {

        requireAdmin(member)

        val normalizedName = Normalizer.normalizeCompany(name)

        require(normalizedName.isNotBlank()) {
            "Company name cannot be normalized (name=$name)"
        }

        // 🔍 조회는 Query 책임
        if (companyQuery.existsByNormalizedName(normalizedName)) {
            throw IllegalStateException(
                "Company already exists (normalizedName=$normalizedName)"
            )
        }

        val company = Company.create(
            name = name,
            source = source,
            externalId = externalId
        )

        return try {
            companyCommand.save(company).id
        } catch (ex: DataIntegrityViolationException) {
            // 동시성 상황에서의 최후 방어 (DB가 진실)
            throw IllegalStateException(
                "Company already exists (normalizedName=$normalizedName)",
                ex
            )
        }
    }

    /**
     * Company 활성화
     */
    @Transactional
    fun activate(
        companyId: Long,
        member: AuthenticatedMember
    ) {

        requireAdmin(member)

        val company = getRequired(companyId)
        company.activate()
        companyCommand.save(company)
    }

    /**
     * Company 비활성화
     */
    @Transactional
    fun deactivate(
        companyId: Long,
        member: AuthenticatedMember
    ) {

        requireAdmin(member)

        val company = getRequired(companyId)
        company.deactivate()
        companyCommand.save(company)

    }

    /**
     * 관리자 권한 검증
     *
     * - 모든 쓰기 유스케이스의 단일 진입점
     */
    private fun requireAdmin(member: AuthenticatedMember) {
        require(member.isAdmin()) {
            "Only ADMIN can operate Company"
        }
    }

    /**
     * 쓰기 유스케이스 전용 필수 조회
     */
    private fun getRequired(companyId: Long): Company =
        companyCommand.findById(companyId)
            ?: throw EntityNotFoundException(
                entityName = "Company",
                identifier = companyId
            )
}
