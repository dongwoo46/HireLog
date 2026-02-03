package com.hirelog.api.brand.application.command

import com.hirelog.api.brand.application.query.BrandQuery
import com.hirelog.api.brand.domain.Brand
import com.hirelog.api.brand.domain.BrandSource
import com.hirelog.api.common.exception.EntityAlreadyExistsException
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.dao.DataIntegrityViolationException

@ExtendWith(MockKExtension::class)
class BrandWriteServiceTest {

    @MockK
    lateinit var brandCommand: BrandCommand

    @MockK
    lateinit var brandQuery: BrandQuery

    private lateinit var brandWriteService: BrandWriteService

    @BeforeEach
    fun setUp() {
        brandWriteService = BrandWriteService(
            brandCommand = brandCommand,
            brandQuery = brandQuery
        )
    }

    private fun createBrand(
        id: Long = 1L,
        name: String = "테스트회사",
        normalizedName: String = "테스트회사"
    ): Brand = mockk {
        every { this@mockk.id } returns id
        every { this@mockk.name } returns name
        every { this@mockk.normalizedName } returns normalizedName
    }

    @Nested
    @DisplayName("getOrCreate")
    inner class GetOrCreateTest {

        @Test
        @DisplayName("존재하지 않는 Brand는 정상적으로 생성된다")
        fun create_when_not_exists() {
            // given
            every { brandQuery.existsByNormalizedName("신규회사") } returns false

            val savedBrand = createBrand()
            every { brandCommand.save(any()) } returns savedBrand

            // when
            val result = brandWriteService.getOrCreate(
                name = "신규회사",
                normalizedName = "신규회사",
                companyId = null,
                source = BrandSource.INFERRED
            )

            // then
            assertEquals(savedBrand, result)
            verify(exactly = 1) { brandCommand.save(any()) }
        }

        @Test
        @DisplayName("이미 존재하는 Brand는 즉시 예외를 발생시킨다")
        fun throw_exception_when_already_exists() {
            // given
            every { brandQuery.existsByNormalizedName("중복회사") } returns true

            // when & then
            assertThrows(EntityAlreadyExistsException::class.java) {
                brandWriteService.getOrCreate(
                    name = "중복회사",
                    normalizedName = "중복회사",
                    companyId = null,
                    source = BrandSource.INFERRED
                )
            }

            verify(exactly = 0) { brandCommand.save(any()) }
        }

        @Test
        @DisplayName("동시성 충돌로 INSERT 실패 시 재조회하여 Brand를 반환한다")
        fun recover_when_data_integrity_violation_occurs() {
            // given
            every { brandQuery.existsByNormalizedName("충돌회사") } returns false
            every { brandCommand.save(any()) } throws DataIntegrityViolationException("duplicate")

            val existingBrand = createBrand()
            every { brandCommand.findByNormalizedName("충돌회사") } returns existingBrand

            // when
            val result = brandWriteService.getOrCreate(
                name = "충돌회사",
                normalizedName = "충돌회사",
                companyId = null,
                source = BrandSource.INFERRED
            )

            // then
            assertEquals(existingBrand, result)
            verify(exactly = 1) { brandCommand.findByNormalizedName("충돌회사") }
        }
    }
}
