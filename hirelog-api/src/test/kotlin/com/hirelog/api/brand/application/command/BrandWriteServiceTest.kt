package com.hirelog.api.brand.application

import com.hirelog.api.brand.application.port.BrandCommand
import com.hirelog.api.brand.domain.Brand
import com.hirelog.api.brand.domain.BrandSource
import com.hirelog.api.common.domain.VerificationStatus
import com.hirelog.api.common.exception.EntityNotFoundException
import com.hirelog.api.common.utils.Normalizer
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.dao.DataIntegrityViolationException

@DisplayName("BrandWriteService 테스트")
class BrandWriteServiceTest {

    private lateinit var brandWriteService: BrandWriteService
    private lateinit var brandCommand: BrandCommand

    @BeforeEach
    fun setUp() {
        brandCommand = mockk()
        brandWriteService = BrandWriteService(brandCommand)
        mockkObject(Normalizer)
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(Normalizer)
    }

    @Nested
    @DisplayName("getOrCreate 메서드는")
    inner class GetOrCreateTest {

        @Test
        @DisplayName("기존 브랜드가 있으면 반환한다")
        fun shouldReturnExistingBrand() {
            // given
            val name = "토스"
            val normalizedName = "toss"
            val companyId = 1L
            val existingBrand = mockk<Brand>()

            every { Normalizer.normalizeBrand(name) } returns normalizedName
            every { brandCommand.findByNormalizedName(normalizedName) } returns existingBrand

            // when
            val result = brandWriteService.getOrCreate(name, companyId, BrandSource.USER)

            // then
            assertThat(result).isEqualTo(existingBrand)
            verify(exactly = 1) { brandCommand.findByNormalizedName(normalizedName) }
            verify(exactly = 0) { brandCommand.save(any()) }
        }

        @Test
        @DisplayName("기존 브랜드가 없으면 새로 생성한다")
        fun shouldCreateNewBrand() {
            // given
            val name = "카카오"
            val normalizedName = "kakao"
            val companyId = 2L
            val source = BrandSource.USER
            val createdBrand = mockk<Brand>()

            every { Normalizer.normalizeBrand(name) } returns normalizedName
            every { brandCommand.findByNormalizedName(normalizedName) } returns null
            every { brandCommand.save(any()) } returns createdBrand

            // when
            val result = brandWriteService.getOrCreate(name, companyId, source)

            // then
            assertThat(result).isEqualTo(createdBrand)
            verify(exactly = 1) { brandCommand.findByNormalizedName(normalizedName) }
            verify(exactly = 1) { brandCommand.save(any()) }
        }

        @Test
        @DisplayName("동시성 충돌 시 재조회하여 반환한다")
        fun shouldRetryOnConcurrencyConflict() {
            // given
            val name = "네이버"
            val normalizedName = "naver"
            val companyId = 3L
            val existingBrand = mockk<Brand>()

            every { Normalizer.normalizeBrand(name) } returns normalizedName
            every { brandCommand.findByNormalizedName(normalizedName) } returnsMany listOf(null, existingBrand)
            every { brandCommand.save(any()) } throws DataIntegrityViolationException("Duplicate key")

            // when
            val result = brandWriteService.getOrCreate(name, companyId, BrandSource.USER)

            // then
            assertThat(result).isEqualTo(existingBrand)
            verify(exactly = 2) { brandCommand.findByNormalizedName(normalizedName) }
            verify(exactly = 1) { brandCommand.save(any()) }
        }

        @Test
        @DisplayName("동시성 충돌 후 재조회에도 실패하면 예외를 던진다")
        fun shouldThrowExceptionWhenRetryFails() {
            // given
            val name = "라인"
            val normalizedName = "line"
            val exception = DataIntegrityViolationException("Constraint violation")

            every { Normalizer.normalizeBrand(name) } returns normalizedName
            every { brandCommand.findByNormalizedName(normalizedName) } returns null
            every { brandCommand.save(any()) } throws exception

            // when & then
            assertThatThrownBy {
                brandWriteService.getOrCreate(name, null, BrandSource.USER)
            }
                .isInstanceOf(DataIntegrityViolationException::class.java)

            verify(exactly = 2) { brandCommand.findByNormalizedName(normalizedName) }
        }
    }

    @Nested
    @DisplayName("create 메서드는")
    inner class CreateTest {

        @Test
        @DisplayName("관리자가 새 브랜드를 생성한다")
        fun shouldCreateBrandByAdmin() {
            // given
            val name = "배민"
            val normalizedName = "baemin"
            val companyId = 4L
            val createdBrand = mockk<Brand>()

            every { Normalizer.normalizeBrand(name) } returns normalizedName
            every { brandCommand.findByNormalizedName(normalizedName) } returns null
            every { brandCommand.save(any()) } returns createdBrand

            // when
            val result = brandWriteService.create(name, companyId)

            // then
            assertThat(result).isEqualTo(createdBrand)
            verify(exactly = 1) { brandCommand.findByNormalizedName(normalizedName) }
            verify(exactly = 1) { brandCommand.save(any()) }
        }

        @Test
        @DisplayName("이미 존재하는 브랜드명으로는 생성할 수 없다")
        fun shouldNotCreateDuplicateBrand() {
            // given
            val name = "당근"
            val normalizedName = "daangn"
            val existingBrand = mockk<Brand>()

            every { Normalizer.normalizeBrand(name) } returns normalizedName
            every { brandCommand.findByNormalizedName(normalizedName) } returns existingBrand

            // when & then
            assertThatThrownBy {
                brandWriteService.create(name, 1L)
            }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("Brand already exists")
                .hasMessageContaining("normalizedName=$normalizedName")

            verify(exactly = 0) { brandCommand.save(any()) }
        }
    }

    @Nested
    @DisplayName("changeName 메서드는")
    inner class ChangeNameTest {

        @Test
        @DisplayName("브랜드명을 변경한다")
        fun shouldChangeBrandName() {
            // given
            val brandId = 1L
            val newName = "토스뱅크"
            val brand = mockk<Brand>(relaxed = true)

            every { brandCommand.findById(brandId) } returns brand
            every { brandCommand.save(brand) } returns brand

            // when
            brandWriteService.changeName(brandId, newName)

            // then
            verify(exactly = 1) { brand.changeName(newName) }
            verify(exactly = 1) { brandCommand.save(brand) }
        }

        @Test
        @DisplayName("이미 존재하는 브랜드명으로는 변경할 수 없다")
        fun shouldNotChangeToExistingBrandName() {
            // given
            val brandId = 1L
            val newName = "카카오"
            val normalizedNewName = "kakao"
            val brand = mockk<Brand>(relaxed = true)
            val existingBrand = mockk<Brand>()

            every { Normalizer.normalizeBrand(newName) } returns normalizedNewName
            every { brandCommand.findById(brandId) } returns brand
            every { brand.changeName(newName) } throws IllegalArgumentException("Brand name already exists")

            // when & then
            assertThatThrownBy {
                brandWriteService.changeName(brandId, newName)
            }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("Brand name already exists")

            verify(exactly = 1) { brand.changeName(newName) }
            verify(exactly = 0) { brandCommand.save(brand) }
        }

        @Test
        @DisplayName("존재하지 않는 브랜드는 변경할 수 없다")
        fun shouldThrowExceptionWhenBrandNotFound() {
            // given
            val brandId = 999L
            val newName = "새이름"

            every { brandCommand.findById(brandId) } returns null

            // when & then
            assertThatThrownBy {
                brandWriteService.changeName(brandId, newName)
            }
                .isInstanceOf(EntityNotFoundException::class.java)
                .hasMessageContaining("Brand")
                .hasMessageContaining("999")
        }
    }

    @Nested
    @DisplayName("verify 메서드는")
    inner class VerifyTest {

        @Test
        @DisplayName("브랜드를 검증 상태로 변경한다")
        fun shouldVerifyBrand() {
            // given
            val brandId = 1L
            val brand = mockk<Brand>(relaxed = true)

            every { brandCommand.findById(brandId) } returns brand
            every { brandCommand.save(brand) } returns brand

            // when
            brandWriteService.verify(brandId)

            // then
            verify(exactly = 1) { brand.verify() }
            verify(exactly = 1) { brandCommand.save(brand) }
        }

        @Test
        @DisplayName("존재하지 않는 브랜드는 검증할 수 없다")
        fun shouldThrowExceptionWhenBrandNotFound() {
            // given
            val brandId = 999L

            every { brandCommand.findById(brandId) } returns null

            // when & then
            assertThatThrownBy {
                brandWriteService.verify(brandId)
            }
                .isInstanceOf(EntityNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("reject 메서드는")
    inner class RejectTest {

        @Test
        @DisplayName("브랜드를 거절 상태로 변경한다")
        fun shouldRejectBrand() {
            // given
            val brandId = 1L
            val brand = mockk<Brand>(relaxed = true)

            every { brandCommand.findById(brandId) } returns brand
            every { brandCommand.save(brand) } returns brand

            // when
            brandWriteService.reject(brandId)

            // then
            verify(exactly = 1) { brand.reject() }
            verify(exactly = 1) { brandCommand.save(brand) }
        }

        @Test
        @DisplayName("존재하지 않는 브랜드는 거절할 수 없다")
        fun shouldThrowExceptionWhenBrandNotFound() {
            // given
            val brandId = 999L

            every { brandCommand.findById(brandId) } returns null

            // when & then
            assertThatThrownBy {
                brandWriteService.reject(brandId)
            }
                .isInstanceOf(EntityNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("activate 메서드는")
    inner class ActivateTest {

        @Test
        @DisplayName("브랜드를 활성화한다")
        fun shouldActivateBrand() {
            // given
            val brandId = 1L
            val brand = mockk<Brand>(relaxed = true)

            every { brandCommand.findById(brandId) } returns brand
            every { brandCommand.save(brand) } returns brand

            // when
            brandWriteService.activate(brandId)

            // then
            verify(exactly = 1) { brand.activate() }
            verify(exactly = 1) { brandCommand.save(brand) }
        }

        @Test
        @DisplayName("존재하지 않는 브랜드는 활성화할 수 없다")
        fun shouldThrowExceptionWhenBrandNotFound() {
            // given
            val brandId = 999L

            every { brandCommand.findById(brandId) } returns null

            // when & then
            assertThatThrownBy {
                brandWriteService.activate(brandId)
            }
                .isInstanceOf(EntityNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("deactivate 메서드는")
    inner class DeactivateTest {

        @Test
        @DisplayName("브랜드를 비활성화한다")
        fun shouldDeactivateBrand() {
            // given
            val brandId = 1L
            val brand = mockk<Brand>(relaxed = true)

            every { brandCommand.findById(brandId) } returns brand
            every { brandCommand.save(brand) } returns brand

            // when
            brandWriteService.deactivate(brandId)

            // then
            verify(exactly = 1) { brand.deactivate() }
            verify(exactly = 1) { brandCommand.save(brand) }
        }

        @Test
        @DisplayName("존재하지 않는 브랜드는 비활성화할 수 없다")
        fun shouldThrowExceptionWhenBrandNotFound() {
            // given
            val brandId = 999L

            every { brandCommand.findById(brandId) } returns null

            // when & then
            assertThatThrownBy {
                brandWriteService.deactivate(brandId)
            }
                .isInstanceOf(EntityNotFoundException::class.java)
        }
    }
}