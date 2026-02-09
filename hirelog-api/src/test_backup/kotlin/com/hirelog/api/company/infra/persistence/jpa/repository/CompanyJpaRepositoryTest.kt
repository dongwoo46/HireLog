package com.hirelog.api.company.infra.persistence.jpa.repository

import com.hirelog.api.company.domain.Company
import com.hirelog.api.company.domain.CompanySource
import com.hirelog.api.testinfra.PostgresJpaTestBase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest

/**
 * CompanyJpaRepository 통합 테스트
 *
 * 검증 대상:
 * - PostgreSQL 기반 DDL 생성
 * - jsonb 컬럼 정상 동작
 * - normalized_name 인덱스 기반 조회
 */
@DataJpaTest
@DisplayName("CompanyJpaRepository (PostgreSQL)")
class CompanyJpaRepositoryTest @Autowired constructor(
    private val companyRepository: CompanyJpaRepository
) : PostgresJpaTestBase() {

    @Test
    @DisplayName("save & findByNormalizedName: 정규화된 이름으로 조회 가능해야 한다")
    fun save_and_find_by_normalized_name() {
        // given
        val company = Company.create(
            name = "Toss",
            source = CompanySource.ADMIN,
            externalId = null
        )

        companyRepository.save(company)

        // when
        val found = companyRepository.findByNormalizedName("toss")

        // then
        assertNotNull(found)
        assertEquals("Toss", found!!.name)
        assertEquals("toss", found.normalizedName)
        assertEquals(CompanySource.ADMIN, found.source)
    }

    @Test
    @DisplayName("existsByNormalizedName: 존재 여부를 올바르게 반환해야 한다")
    fun exists_by_normalized_name() {
        // given
        val company = Company.create(
            name = "Kakao",
            source = CompanySource.ADMIN,
            externalId = null
        )

        companyRepository.save(company)

        // then
        assertEquals(true, companyRepository.existsByNormalizedName("kakao"))
        assertEquals(false, companyRepository.existsByNormalizedName("naver"))
    }
}
