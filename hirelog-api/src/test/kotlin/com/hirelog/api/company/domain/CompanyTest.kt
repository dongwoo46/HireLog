package com.hirelog.api.company.domain

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.*

@DisplayName("Company 도메인 테스트")
class CompanyTest {

    private fun makeCompany(name: String = "비바리퍼블리카"): Company =
        Company.create(name = name, source = CompanySource.ADMIN, externalId = null)

    @Nested
    @DisplayName("create 팩토리는")
    inner class CreateTest {

        @Test
        @DisplayName("isActive=true로 생성하고 normalizedName을 계산한다")
        fun shouldCreateActiveCompany() {
            val company = makeCompany("비바리퍼블리카")

            assertThat(company.name).isEqualTo("비바리퍼블리카")
            assertThat(company.isActive).isTrue()
            assertThat(company.normalizedName).isNotBlank()
        }

        @Test
        @DisplayName("법인 접미사를 제거하여 normalizedName을 생성한다")
        fun shouldRemoveCompanySuffixInNormalizedName() {
            val company = makeCompany("(주)비바리퍼블리카")
            // Normalizer.normalizeCompany: (주) 제거 + 허용문자만
            assertThat(company.normalizedName).doesNotContain("주")
        }
    }

    @Nested
    @DisplayName("deactivate는")
    inner class DeactivateTest {

        @Test
        @DisplayName("isActive=true이면 false로 변경한다")
        fun shouldDeactivate() {
            val company = makeCompany()
            company.deactivate()
            assertThat(company.isActive).isFalse()
        }

        @Test
        @DisplayName("이미 비활성화된 경우 무시한다 (멱등)")
        fun shouldBeIdempotent() {
            val company = makeCompany()
            company.deactivate()
            company.deactivate()
            assertThat(company.isActive).isFalse()
        }
    }

    @Nested
    @DisplayName("activate는")
    inner class ActivateTest {

        @Test
        @DisplayName("isActive=false이면 true로 변경한다")
        fun shouldActivate() {
            val company = makeCompany()
            company.deactivate()
            company.activate()
            assertThat(company.isActive).isTrue()
        }

        @Test
        @DisplayName("이미 활성화된 경우 무시한다 (멱등)")
        fun shouldBeIdempotent() {
            val company = makeCompany()
            company.activate()
            assertThat(company.isActive).isTrue()
        }
    }

    @Nested
    @DisplayName("changeName은")
    inner class ChangeNameTest {

        @Test
        @DisplayName("이름을 변경하고 normalizedName도 업데이트한다")
        fun shouldChangeNameAndNormalizedName() {
            val company = makeCompany("비바리퍼블리카")
            val oldNormalized = company.normalizedName

            company.changeName("토스페이먼츠")

            assertThat(company.name).isEqualTo("토스페이먼츠")
            assertThat(company.normalizedName).isNotEqualTo(oldNormalized)
        }

        @Test
        @DisplayName("동일한 이름으로 변경하면 무시한다")
        fun shouldIgnoreWhenSameName() {
            val company = makeCompany("비바리퍼블리카")
            val originalNormalized = company.normalizedName

            company.changeName("비바리퍼블리카")

            assertThat(company.normalizedName).isEqualTo(originalNormalized)
        }

        @Test
        @DisplayName("blank 이름이면 예외를 던진다")
        fun shouldThrowWhenBlank() {
            val company = makeCompany()

            assertThatThrownBy { company.changeName("  ") }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("blank")
        }
    }
}
