package com.hirelog.api.brand.application.command

import com.hirelog.api.brand.domain.Brand
import com.hirelog.api.brand.domain.BrandSource
import com.hirelog.api.common.exception.EntityNotFoundException
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.dao.DataIntegrityViolationException

@DisplayName("BrandWriteService 테스트")
class BrandWriteServiceTest {

    private val brandCommand: BrandCommand = mockk(relaxed = true)
    private val service = BrandWriteService(brandCommand)

    @Test
    @DisplayName("getOrCreate: 이미 존재하면 저장하지 않고 기존 Brand를 반환한다")
    fun getOrCreate_when_exists_should_return_existing() {
        // given
        val normalizedName = "toss"
        val existingBrand = mockk<Brand>()

        every { brandCommand.findByNormalizedName(normalizedName) } returns existingBrand

        // when
        val result = service.getOrCreate(
            name = "Toss",
            normalizedName = normalizedName,
            companyId = null,
            source = BrandSource.USER
        )

        // then
        assertEquals(existingBrand, result)
        verify(exactly = 0) { brandCommand.save(any()) }
    }

    @Test
    @DisplayName("getOrCreate: 존재하지 않으면 Brand를 생성하여 저장한다")
    fun getOrCreate_when_not_exists_should_create_and_return() {
        // given
        val normalizedName = "new_brand"
        val savedBrand = mockk<Brand>()

        every { brandCommand.findByNormalizedName(normalizedName) } returns null
        every { brandCommand.save(any()) } returns savedBrand

        // when
        val result = service.getOrCreate(
            name = "New Brand",
            normalizedName = normalizedName,
            companyId = null,
            source = BrandSource.USER
        )

        // then
        assertEquals(savedBrand, result)
        verify(exactly = 1) { brandCommand.save(any()) }
    }

    @Test
    @DisplayName("getOrCreate: 동시성 충돌 시 재조회하여 기존 Brand를 반환한다")
    fun getOrCreate_when_conflict_should_requery_and_return_existing() {
        // given
        val normalizedName = "concurrent_brand"
        val existingBrand = mockk<Brand>()

        every {
            brandCommand.findByNormalizedName(normalizedName)
        } returnsMany listOf(null, existingBrand)

        every {
            brandCommand.save(any())
        } throws DataIntegrityViolationException("duplicate key")

        // when
        val result = service.getOrCreate(
            name = "Concurrent Brand",
            normalizedName = normalizedName,
            companyId = null,
            source = BrandSource.USER
        )

        // then
        assertEquals(existingBrand, result)
        verify(exactly = 1) { brandCommand.save(any()) }
        verify(exactly = 2) { brandCommand.findByNormalizedName(normalizedName) }
    }

    @Test
    @DisplayName("verify: 브랜드가 존재하면 verify()가 호출된다")
    fun verify_when_exists_should_call_verify() {
        // given
        val brandId = 1L
        val brand = mockk<Brand>(relaxed = true)

        every { brandCommand.findById(brandId) } returns brand

        // when
        service.verify(brandId)

        // then
        verify(exactly = 1) { brand.verify() }
    }

    @Test
    @DisplayName("verify: 브랜드가 없으면 EntityNotFoundException이 발생한다")
    fun verify_when_not_exists_should_throw_exception() {
        // given
        val brandId = 999L
        every { brandCommand.findById(brandId) } returns null

        // when & then
        assertThrows(EntityNotFoundException::class.java) {
            service.verify(brandId)
        }
    }
}

