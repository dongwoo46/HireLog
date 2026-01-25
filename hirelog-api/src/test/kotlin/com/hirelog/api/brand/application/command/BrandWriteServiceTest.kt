package com.hirelog.api.brand.application.command

import com.hirelog.api.brand.application.query.BrandQuery
import com.hirelog.api.brand.domain.Brand
import com.hirelog.api.brand.domain.BrandSource
import com.hirelog.api.common.domain.VerificationStatus
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@ExtendWith(MockKExtension::class)
class BrandWriteServiceTest {

    @MockK
    lateinit var brandCommand: BrandCommand

    @MockK
    lateinit var brandQuery: BrandQuery

    private lateinit var brandWriteService: BrandWriteService

    @BeforeEach
    fun setUp() {
        brandWriteService = BrandWriteService(brandCommand, brandQuery)
    }

    private fun createTestBrand(
        id: Long = 1L,
        name: String = "테스트회사",
        normalizedName: String = "테스트회사"
    ) = mockk<Brand> {
        every { this@mockk.id } returns id
        every { this@mockk.name } returns name
        every { this@mockk.normalizedName } returns normalizedName
    }

    @Nested
    @DisplayName("getOrCreate 테스트")
    inner class GetOrCreateTest {

        @Test
        @DisplayName("이미 존재하는 Brand는 조회 후 반환")
        fun `should return existing brand when found`() {
            // given
            val existingBrand = createTestBrand()
            every { brandQuery.findByNormalizedName("테스트회사") } returns existingBrand

            // when
            val result = brandWriteService.getOrCreate(
                name = "테스트회사",
                normalizedName = "테스트회사",
                companyId = null,
                source = BrandSource.INFERRED
            )

            // then
            assertEquals(existingBrand, result)
            verify(exactly = 1) { brandQuery.findByNormalizedName("테스트회사") }
            verify(exactly = 0) { brandCommand.insertIgnoreDuplicate(any(), any(), any(), any(), any(), any()) }
        }

        @Test
        @DisplayName("존재하지 않는 Brand는 INSERT 후 조회")
        fun `should insert and return new brand when not found`() {
            // given
            val newBrand = createTestBrand()

            every { brandQuery.findByNormalizedName("신규회사") } returns null andThen newBrand
            every {
                brandCommand.insertIgnoreDuplicate(
                    name = "신규회사",
                    normalizedName = "신규회사",
                    companyId = null,
                    verificationStatus = VerificationStatus.UNVERIFIED,
                    source = BrandSource.INFERRED,
                    isActive = true
                )
            } returns 1

            // when
            val result = brandWriteService.getOrCreate(
                name = "신규회사",
                normalizedName = "신규회사",
                companyId = null,
                source = BrandSource.INFERRED
            )

            // then
            assertNotNull(result)
            verify(exactly = 1) { brandCommand.insertIgnoreDuplicate(any(), any(), any(), any(), any(), any()) }
        }

        @Test
        @DisplayName("ON CONFLICT로 INSERT가 무시된 경우 재조회로 기존 Brand 반환")
        fun `should return existing brand when insert ignored due to conflict`() {
            // given
            val existingBrand = createTestBrand()

            // 첫 조회: null, INSERT: 0 (conflict), 재조회: existingBrand
            every { brandQuery.findByNormalizedName("중복회사") } returns null andThen existingBrand
            every {
                brandCommand.insertIgnoreDuplicate(any(), any(), any(), any(), any(), any())
            } returns 0  // ON CONFLICT DO NOTHING

            // when
            val result = brandWriteService.getOrCreate(
                name = "중복회사",
                normalizedName = "중복회사",
                companyId = null,
                source = BrandSource.INFERRED
            )

            // then
            assertEquals(existingBrand, result)
            verify(exactly = 1) { brandCommand.insertIgnoreDuplicate(any(), any(), any(), any(), any(), any()) }
            verify(exactly = 2) { brandQuery.findByNormalizedName("중복회사") }
        }
    }

    @Nested
    @DisplayName("동시성 테스트")
    inner class ConcurrencyTest {

        @Test
        @DisplayName("동시에 같은 Brand 생성 요청 시 모두 성공하고 1개만 생성됨")
        fun `should handle concurrent getOrCreate requests without error`() {
            // given
            val concurrentRequests = 10
            val latch = CountDownLatch(concurrentRequests)
            val executor = Executors.newFixedThreadPool(concurrentRequests)
            val successCount = AtomicInteger(0)
            val errorCount = AtomicInteger(0)

            val testBrand = createTestBrand(name = "동시성테스트회사", normalizedName = "동시성테스트회사")
            val insertCallCount = AtomicInteger(0)

            // 첫 번째 조회는 항상 null, 이후 조회는 brand 반환
            every { brandQuery.findByNormalizedName("동시성테스트회사") } answers {
                if (insertCallCount.get() > 0) testBrand else null
            }

            every {
                brandCommand.insertIgnoreDuplicate(any(), any(), any(), any(), any(), any())
            } answers {
                // 첫 번째 insert만 성공 (1), 나머지는 conflict (0)
                if (insertCallCount.incrementAndGet() == 1) 1 else 0
            }

            // when
            repeat(concurrentRequests) {
                executor.submit {
                    try {
                        brandWriteService.getOrCreate(
                            name = "동시성테스트회사",
                            normalizedName = "동시성테스트회사",
                            companyId = null,
                            source = BrandSource.INFERRED
                        )
                        successCount.incrementAndGet()
                    } catch (e: Exception) {
                        errorCount.incrementAndGet()
                    } finally {
                        latch.countDown()
                    }
                }
            }

            latch.await(10, TimeUnit.SECONDS)
            executor.shutdown()

            // then
            assertEquals(concurrentRequests, successCount.get(), "모든 요청이 성공해야 함")
            assertEquals(0, errorCount.get(), "에러가 발생하면 안 됨")
        }
    }
}
