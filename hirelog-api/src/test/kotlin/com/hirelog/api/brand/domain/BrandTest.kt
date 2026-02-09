package com.hirelog.api.brand.domain

import com.hirelog.api.common.domain.VerificationStatus
import com.hirelog.api.common.utils.Normalizer
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("Brand 도메인 테스트")
class BrandTest {

    @BeforeEach
    fun setUp() {
        mockkObject(Normalizer)
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(Normalizer)
    }

    @Nested
    @DisplayName("create 메서드는")
    inner class CreateTest {

        @Test
        @DisplayName("일반 브랜드를 생성한다")
        fun shouldCreateBrand() {
            // given
            val name = "토스"
            val companyId = 1L
            val source = BrandSource.USER
            val normalizedName = "toss"

            every { Normalizer.normalizeBrand(name) } returns normalizedName

            // when
            val brand = Brand.create(name, companyId, source)

            // then
            assertThat(brand.name).isEqualTo(name)
            assertThat(brand.normalizedName).isEqualTo(normalizedName)
            assertThat(brand.companyId).isEqualTo(companyId)
            assertThat(brand.verificationStatus).isEqualTo(VerificationStatus.UNVERIFIED)
            assertThat(brand.source).isEqualTo(source)
            assertThat(brand.isActive).isTrue()
        }

        @Test
        @DisplayName("companyId 없이 브랜드를 생성할 수 있다")
        fun shouldCreateBrandWithoutCompanyId() {
            // given
            val name = "강남언니"
            val normalizedName = "gangnamunni"

            every { Normalizer.normalizeBrand(name) } returns normalizedName

            // when
            val brand = Brand.create(name, null, BrandSource.USER)

            // then
            assertThat(brand.companyId).isNull()
            assertThat(brand.verificationStatus).isEqualTo(VerificationStatus.UNVERIFIED)
        }
    }

    @Nested
    @DisplayName("createByAdmin 메서드는")
    inner class CreateByAdminTest {

        @Test
        @DisplayName("관리자가 검증된 상태의 브랜드를 생성한다")
        fun shouldCreateVerifiedBrandByAdmin() {
            // given
            val name = "카카오"
            val companyId = 2L
            val normalizedName = "kakao"

            every { Normalizer.normalizeBrand(name) } returns normalizedName

            // when
            val brand = Brand.createByAdmin(name, companyId)

            // then
            assertThat(brand.name).isEqualTo(name)
            assertThat(brand.normalizedName).isEqualTo(normalizedName)
            assertThat(brand.companyId).isEqualTo(companyId)
            assertThat(brand.verificationStatus).isEqualTo(VerificationStatus.VERIFIED)
            assertThat(brand.source).isEqualTo(BrandSource.ADMIN)
            assertThat(brand.isActive).isTrue()
        }

        @Test
        @DisplayName("관리자가 companyId 없이 브랜드를 생성할 수 있다")
        fun shouldCreateBrandByAdminWithoutCompanyId() {
            // given
            val name = "네이버"
            val normalizedName = "naver"

            every { Normalizer.normalizeBrand(name) } returns normalizedName

            // when
            val brand = Brand.createByAdmin(name, null)

            // then
            assertThat(brand.companyId).isNull()
            assertThat(brand.verificationStatus).isEqualTo(VerificationStatus.VERIFIED)
            assertThat(brand.source).isEqualTo(BrandSource.ADMIN)
        }
    }

    @Nested
    @DisplayName("verify 메서드는")
    inner class VerifyTest {

        @Test
        @DisplayName("미검증 브랜드를 검증 상태로 변경한다")
        fun shouldVerifyUnverifiedBrand() {
            // given
            val name = "토스"
            every { Normalizer.normalizeBrand(name) } returns "toss"

            val brand = Brand.create(name, 1L, BrandSource.USER)

            // when
            brand.verify()

            // then
            assertThat(brand.verificationStatus).isEqualTo(VerificationStatus.VERIFIED)
            assertThat(brand.isActive).isTrue()
        }

        @Test
        @DisplayName("거절된 브랜드를 검증 상태로 변경한다")
        fun shouldVerifyRejectedBrand() {
            // given
            val name = "토스"
            every { Normalizer.normalizeBrand(name) } returns "toss"

            val brand = Brand.create(name, 1L, BrandSource.USER)
            brand.reject()

            // when
            brand.verify()

            // then
            assertThat(brand.verificationStatus).isEqualTo(VerificationStatus.VERIFIED)
            assertThat(brand.isActive).isTrue()
        }

        @Test
        @DisplayName("이미 검증된 브랜드는 상태가 변경되지 않는다")
        fun shouldNotChangeAlreadyVerifiedBrand() {
            // given
            val name = "토스"
            every { Normalizer.normalizeBrand(name) } returns "toss"

            val brand = Brand.createByAdmin(name, 1L)

            // when
            brand.verify()

            // then
            assertThat(brand.verificationStatus).isEqualTo(VerificationStatus.VERIFIED)
        }
    }

    @Nested
    @DisplayName("reject 메서드는")
    inner class RejectTest {

        @Test
        @DisplayName("미검증 브랜드를 거절 상태로 변경한다")
        fun shouldRejectUnverifiedBrand() {
            // given
            val name = "토스"
            every { Normalizer.normalizeBrand(name) } returns "toss"

            val brand = Brand.create(name, 1L, BrandSource.USER)

            // when
            brand.reject()

            // then
            assertThat(brand.verificationStatus).isEqualTo(VerificationStatus.REJECTED)
            assertThat(brand.isActive).isFalse()
        }

        @Test
        @DisplayName("검증된 브랜드를 거절 상태로 변경한다")
        fun shouldRejectVerifiedBrand() {
            // given
            val name = "토스"
            every { Normalizer.normalizeBrand(name) } returns "toss"

            val brand = Brand.createByAdmin(name, 1L)

            // when
            brand.reject()

            // then
            assertThat(brand.verificationStatus).isEqualTo(VerificationStatus.REJECTED)
            assertThat(brand.isActive).isFalse()
        }

        @Test
        @DisplayName("이미 거절된 브랜드는 상태가 변경되지 않는다")
        fun shouldNotChangeAlreadyRejectedBrand() {
            // given
            val name = "토스"
            every { Normalizer.normalizeBrand(name) } returns "toss"

            val brand = Brand.create(name, 1L, BrandSource.USER)
            brand.reject()

            // when
            brand.reject()

            // then
            assertThat(brand.verificationStatus).isEqualTo(VerificationStatus.REJECTED)
            assertThat(brand.isActive).isFalse()
        }
    }

    @Nested
    @DisplayName("deactivate 메서드는")
    inner class DeactivateTest {

        @Test
        @DisplayName("활성 브랜드를 비활성화한다")
        fun shouldDeactivateActiveBrand() {
            // given
            val name = "토스"
            every { Normalizer.normalizeBrand(name) } returns "toss"

            val brand = Brand.createByAdmin(name, 1L)

            // when
            brand.deactivate()

            // then
            assertThat(brand.isActive).isFalse()
        }

        @Test
        @DisplayName("이미 비활성화된 브랜드는 상태가 변경되지 않는다")
        fun shouldNotChangeAlreadyDeactivatedBrand() {
            // given
            val name = "토스"
            every { Normalizer.normalizeBrand(name) } returns "toss"

            val brand = Brand.createByAdmin(name, 1L)
            brand.deactivate()

            // when
            brand.deactivate()

            // then
            assertThat(brand.isActive).isFalse()
        }
    }

    @Nested
    @DisplayName("activate 메서드는")
    inner class ActivateTest {

        @Test
        @DisplayName("검증된 비활성 브랜드를 활성화한다")
        fun shouldActivateVerifiedInactiveBrand() {
            // given
            val name = "토스"
            every { Normalizer.normalizeBrand(name) } returns "toss"

            val brand = Brand.createByAdmin(name, 1L)
            brand.deactivate()

            // when
            brand.activate()

            // then
            assertThat(brand.isActive).isTrue()
        }

        @Test
        @DisplayName("이미 활성화된 브랜드는 상태가 변경되지 않는다")
        fun shouldNotChangeAlreadyActiveBrand() {
            // given
            val name = "토스"
            every { Normalizer.normalizeBrand(name) } returns "toss"

            val brand = Brand.createByAdmin(name, 1L)

            // when
            brand.activate()

            // then
            assertThat(brand.isActive).isTrue()
        }

        @Test
        @DisplayName("미검증 브랜드는 활성화할 수 없다")
        fun shouldNotActivateUnverifiedBrand() {
            // given
            val name = "토스"
            every { Normalizer.normalizeBrand(name) } returns "toss"

            val brand = Brand.create(name, 1L, BrandSource.USER)

            // when & then
            assertThatThrownBy { brand.activate() }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("Only verified brand can be activated")
                .hasMessageContaining("current=UNVERIFIED")
        }

        @Test
        @DisplayName("거절된 브랜드는 활성화할 수 없다")
        fun shouldNotActivateRejectedBrand() {
            // given
            val name = "토스"
            every { Normalizer.normalizeBrand(name) } returns "toss"

            val brand = Brand.create(name, 1L, BrandSource.USER)
            brand.reject()

            // when & then
            assertThatThrownBy { brand.activate() }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("Only verified brand can be activated")
                .hasMessageContaining("current=REJECTED")
        }
    }

    @Nested
    @DisplayName("changeName 메서드는")
    inner class ChangeNameTest {

        @Test
        @DisplayName("브랜드명을 변경한다")
        fun shouldChangeBrandName() {
            // given
            val oldName = "토스"
            val newName = "토스뱅크"
            every { Normalizer.normalizeBrand(oldName) } returns "toss"
            every { Normalizer.normalizeBrand(newName) } returns "tossbank"

            val brand = Brand.createByAdmin(oldName, 1L)

            // when
            brand.changeName(newName)

            // then
            assertThat(brand.name).isEqualTo(newName)
            assertThat(brand.normalizedName).isEqualTo("tossbank")
        }

        @Test
        @DisplayName("동일한 이름으로 변경 시 무시된다")
        fun shouldIgnoreSameNameChange() {
            // given
            val name = "토스"
            val normalizedName = "toss"
            every { Normalizer.normalizeBrand(name) } returns normalizedName

            val brand = Brand.createByAdmin(name, 1L)

            // when
            brand.changeName(name)

            // then
            assertThat(brand.name).isEqualTo(name)
            assertThat(brand.normalizedName).isEqualTo(normalizedName)
        }

        @Test
        @DisplayName("빈 문자열로 변경할 수 없다")
        fun shouldNotChangeToBlankName() {
            // given
            val name = "토스"
            every { Normalizer.normalizeBrand(name) } returns "toss"

            val brand = Brand.createByAdmin(name, 1L)

            // when & then
            assertThatThrownBy { brand.changeName("") }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("Brand name must not be blank")
        }

        @Test
        @DisplayName("공백 문자열로 변경할 수 없다")
        fun shouldNotChangeToWhitespaceName() {
            // given
            val name = "토스"
            every { Normalizer.normalizeBrand(name) } returns "toss"

            val brand = Brand.createByAdmin(name, 1L)

            // when & then
            assertThatThrownBy { brand.changeName("   ") }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("Brand name must not be blank")
        }
    }
}