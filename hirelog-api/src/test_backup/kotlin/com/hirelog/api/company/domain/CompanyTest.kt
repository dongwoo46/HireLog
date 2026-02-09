package com.hirelog.api.company.domain

import com.hirelog.api.common.domain.VerificationStatus
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("Company 도메인 테스트")
class CompanyTest {

    @Nested
    @DisplayName("Company 생성 테스트")
    inner class CreateTest {

        @Test
        @DisplayName("create: 초기 상태는 UNVERIFIED, isActive=true로 생성되어야 한다")
        fun create_success() {
            // given
            val name = "Toss Corp"
            val aliases = listOf("Toss", "비바리퍼블리카")
            val source = CompanySource.ADMIN
            val externalId = "123-45-67890"

            // when
            val company = Company.create(name,  source,externalId, aliases)

            // then
            assertEquals("Toss Corp", company.name)
            // normalize 로직 검증 (lowercase + alphanumeric + underscore)
            assertEquals("toss_corp", company.normalizedName)
            assertEquals(aliases, company.aliases)
            assertEquals(source, company.source)
            assertEquals(externalId, company.externalId)
            assertEquals(VerificationStatus.UNVERIFIED, company.verificationStatus)
            assertTrue(company.isActive)
        }

        @Test
        @DisplayName("normalize: 특수문자는 제거되고 공백은 _로 치환되어야 한다")
        fun normalize_logic() {
            // given
            val rawName = "Toss & Co. (Inc)"

            // when
            val normalized = Company.normalize(rawName)

            // then
            // toss _ co _ inc -> toss_co_inc (trim logic 확인 필요)
            // 구현: replace(Regex("[^a-z0-9]+"), "_") -> toss_co_inc_
            // trim('_') -> toss_co_inc
            assertEquals("toss_co_inc", normalized)
        }
    }

    @Nested
    @DisplayName("상태 변경 테스트")
    inner class StateChangeTest {

        @Test
        @DisplayName("verify: 검증되지 않은 회사를 승인하면 VERIFIED 상태가 되어야 한다")
        fun verify_success() {
            // given
            val company = Company.create("Test", CompanySource.ADMIN, null)

            assertEquals(VerificationStatus.UNVERIFIED, company.verificationStatus)

            // when
            company.verify()

            // then
            assertEquals(VerificationStatus.VERIFIED, company.verificationStatus)
        }

        @Test
        @DisplayName("verify: 이미 승인된 회사는 상태가 변경되지 않아야 한다")
        fun verify_already_verified() {
            // given
            val company = Company.create("Test", CompanySource.ADMIN, null)
            company.verify()

            // when
            company.verify()

            // then
            assertEquals(VerificationStatus.VERIFIED, company.verificationStatus)
        }

        @Test
        @DisplayName("deactivate: 활성 회사를 비활성화하면 isActive가 false가 되어야 한다")
        fun deactivate_success() {
            // given
            val company = Company.create("Test", CompanySource.ADMIN, null)
            assertTrue(company.isActive)

            // when
            company.deactivate()

            // then
            assertFalse(company.isActive)
        }

        @Test
        @DisplayName("deactivate: 이미 비활성화된 회사는 상태가 유지되어야 한다")
        fun deactivate_already_inactive() {
            // given
            val company = Company.create("Test", CompanySource.ADMIN, null)
            company.deactivate()

            // when
            company.deactivate()

            // then
            assertFalse(company.isActive)
        }
    }
}
