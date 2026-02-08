package com.hirelog.api.position.application

import com.hirelog.api.common.exception.EntityAlreadyExistsException
import com.hirelog.api.common.exception.EntityNotFoundException
import com.hirelog.api.position.application.port.PositionCommand
import com.hirelog.api.position.domain.Position
import com.hirelog.api.position.domain.PositionCategory
import com.hirelog.api.position.domain.PositionStatus
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.dao.DataIntegrityViolationException

@DisplayName("PositionWriteService")
class PositionWriteServiceTest {

    private val positionCommand: PositionCommand = mockk()
    private lateinit var service: PositionWriteService

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        service = PositionWriteService(positionCommand)
    }

    /* =========================
     * Test Fixtures
     * ========================= */

    private fun createPositionCategory(
        id: Long = 1L,
        name: String = "Engineering"
    ) = mockk<PositionCategory> {
        every { this@mockk.id } returns id
        every { this@mockk.name } returns name
    }

    private fun createPosition(
        id: Long = 1L,
        name: String = "Backend Engineer",
        normalizedName: String = "backend_engineer",
        category: PositionCategory = createPositionCategory(),
        status: PositionStatus = PositionStatus.ACTIVE,
        description: String? = null
    ) = mockk<Position>(relaxed = true) {
        every { this@mockk.id } returns id
        every { this@mockk.name } returns name
        every { this@mockk.normalizedName } returns normalizedName
        every { this@mockk.category } returns category  // ✅ positionCategory → category
        every { this@mockk.status } returns status
        every { this@mockk.description } returns description
    }

    /* =========================
     * getOrCreate
     * ========================= */

    @Nested
    @DisplayName("getOrCreate")
    inner class GetOrCreate {

        @Test
        fun `존재하는 Position이면 그대로 반환`() {
            // Given
            val normalizedName = "backend_engineer"
            val category = createPositionCategory()
            val existingPosition = createPosition(
                id = 10L,
                normalizedName = normalizedName,
                category = category
            )

            every { positionCommand.findByNormalizedName(normalizedName) } returns existingPosition

            // When
            val result = service.getOrCreate("Backend Engineer", category, normalizedName)

            // Then
            assertEquals(10L, result.id)
            assertEquals(normalizedName, result.normalizedName)
            verify(exactly = 1) { positionCommand.findByNormalizedName(normalizedName) }
            verify(exactly = 0) { positionCommand.save(any()) }
        }

        @Test
        fun `존재하지 않으면 신규 생성`() {
            // Given
            val normalizedName = "new_position"
            val category = createPositionCategory()

            every { positionCommand.findByNormalizedName(normalizedName) } returns null

            val savedPosition = createPosition(
                id = 20L,
                normalizedName = normalizedName,
                category = category,
                status = PositionStatus.CANDIDATE  // 신규 생성 시 CANDIDATE
            )
            every { positionCommand.save(any()) } returns savedPosition

            // When
            val result = service.getOrCreate("New Position", category, normalizedName)

            // Then
            assertEquals(20L, result.id)
            assertEquals(normalizedName, result.normalizedName)
            verify(exactly = 1) { positionCommand.findByNormalizedName(normalizedName) }
            verify(exactly = 1) { positionCommand.save(any()) }
        }

        @Test
        fun `동시 생성 시 DataIntegrityViolationException 발생하면 재조회`() {
            // Given
            val normalizedName = "concurrent_position"
            val category = createPositionCategory()

            val concurrentPosition = createPosition(
                id = 30L,
                normalizedName = normalizedName,
                category = category
            )

            // 첫 번째 조회: 없음, 두 번째 조회: 다른 스레드가 생성한 Position
            every { positionCommand.findByNormalizedName(normalizedName) } returns null andThen concurrentPosition

            // 저장 시도에서 DB 제약 위반 (다른 스레드가 먼저 생성)
            every { positionCommand.save(any()) } throws DataIntegrityViolationException("Duplicate key")

            // When
            val result = service.getOrCreate("Concurrent Position", category, normalizedName)

            // Then
            assertEquals(30L, result.id)
            verify(exactly = 2) { positionCommand.findByNormalizedName(normalizedName) }
            verify(exactly = 1) { positionCommand.save(any()) }
        }

        @Test
        fun `동시 생성 시도 후 재조회에서도 없으면 예외 전파`() {
            // Given
            val normalizedName = "error_position"
            val category = createPositionCategory()

            every { positionCommand.findByNormalizedName(normalizedName) } returns null

            val exception = DataIntegrityViolationException("Unknown constraint violation")
            every { positionCommand.save(any()) } throws exception

            // When & Then
            assertThrows(DataIntegrityViolationException::class.java) {
                service.getOrCreate("Error Position", category, normalizedName)
            }
        }
    }

    /* =========================
     * create
     * ========================= */

    @Nested
    @DisplayName("create")
    inner class Create {

        @Test
        fun `정상적으로 Position 생성`() {
            // Given
            val name = "Frontend Engineer"
            val category = createPositionCategory()
            val description = "Develops frontend applications"

            val savedPosition = createPosition(
                id = 100L,
                name = name,
                category = category,
                description = description,
                status = PositionStatus.CANDIDATE
            )
            every { positionCommand.save(any()) } returns savedPosition

            // When
            val result = service.create(name, category, description)

            // Then
            assertEquals(100L, result.id)
            assertEquals(name, result.name)
            verify(exactly = 1) { positionCommand.save(any()) }
        }

        @Test
        fun `중복된 normalizedName으로 생성 시 EntityAlreadyExistsException`() {
            // Given
            val name = "Duplicate Position"
            val category = createPositionCategory()

            every { positionCommand.save(any()) } throws DataIntegrityViolationException("Duplicate key")

            // When & Then
            val exception = assertThrows(EntityAlreadyExistsException::class.java) {
                service.create(name, category, null)
            }

            assertTrue(exception.message!!.contains("Position already exists"))
            assertTrue(exception.message!!.contains("normalized"))
        }

        @Test
        fun `description이 null이어도 생성 가능`() {
            // Given
            val name = "Simple Position"
            val category = createPositionCategory()

            val savedPosition = createPosition(
                id = 200L,
                name = name,
                category = category,
                description = null
            )
            every { positionCommand.save(any()) } returns savedPosition

            // When
            val result = service.create(name, category, null)

            // Then
            assertNotNull(result)
            assertEquals(200L, result.id)
            assertNull(result.description)
        }
    }

    /* =========================
     * activate
     * ========================= */

    @Nested
    @DisplayName("activate")
    inner class Activate {

        @Test
        fun `CANDIDATE 상태 Position을 ACTIVE로 활성화`() {
            // Given
            val positionId = 10L
            val position = createPosition(
                id = positionId,
                status = PositionStatus.CANDIDATE
            )

            every { positionCommand.findById(positionId) } returns position
            every { position.activate() } just Runs

            // When
            service.activate(positionId)

            // Then
            verify(exactly = 1) { positionCommand.findById(positionId) }
            verify(exactly = 1) { position.activate() }
        }

        @Test
        fun `DEPRECATED 상태 Position을 ACTIVE로 재활성화`() {
            // Given
            val positionId = 15L
            val position = createPosition(
                id = positionId,
                status = PositionStatus.DEPRECATED
            )

            every { positionCommand.findById(positionId) } returns position
            every { position.activate() } just Runs

            // When
            service.activate(positionId)

            // Then
            verify(exactly = 1) { position.activate() }
        }

        @Test
        fun `존재하지 않는 Position 활성화 시 EntityNotFoundException`() {
            // Given
            val positionId = 999L
            every { positionCommand.findById(positionId) } returns null

            // When & Then
            val exception = assertThrows(EntityNotFoundException::class.java) {
                service.activate(positionId)
            }

            assertTrue(exception.message!!.contains("Position"))
            assertTrue(exception.message!!.contains("999"))
        }

        @Test
        fun `이미 ACTIVE 상태인 Position 활성화는 무시됨`() {
            // Given
            val positionId = 20L
            val position = createPosition(
                id = positionId,
                status = PositionStatus.ACTIVE
            )

            every { positionCommand.findById(positionId) } returns position
            every { position.activate() } just Runs

            // When
            service.activate(positionId)

            // Then
            verify(exactly = 1) { position.activate() }
        }
    }

    /* =========================
     * deprecate
     * ========================= */

    @Nested
    @DisplayName("deprecate")
    inner class Deprecate {

        @Test
        fun `ACTIVE 상태 Position을 DEPRECATED로 비활성화`() {
            // Given
            val positionId = 30L
            val position = createPosition(
                id = positionId,
                status = PositionStatus.ACTIVE
            )

            every { positionCommand.findById(positionId) } returns position
            every { position.deprecate() } just Runs

            // When
            service.deprecate(positionId)

            // Then
            verify(exactly = 1) { positionCommand.findById(positionId) }
            verify(exactly = 1) { position.deprecate() }
        }

        @Test
        fun `존재하지 않는 Position 비활성화 시 EntityNotFoundException`() {
            // Given
            val positionId = 888L
            every { positionCommand.findById(positionId) } returns null

            // When & Then
            val exception = assertThrows(EntityNotFoundException::class.java) {
                service.deprecate(positionId)
            }

            assertTrue(exception.message!!.contains("Position"))
            assertTrue(exception.message!!.contains("888"))
        }

        @Test
        fun `이미 DEPRECATED 상태인 Position 비활성화는 무시됨`() {
            // Given
            val positionId = 35L
            val position = createPosition(
                id = positionId,
                status = PositionStatus.DEPRECATED
            )

            every { positionCommand.findById(positionId) } returns position
            every { position.deprecate() } just Runs

            // When
            service.deprecate(positionId)

            // Then
            verify(exactly = 1) { position.deprecate() }
        }
    }

    /* =========================
     * 복합 시나리오
     * ========================= */

    @Nested
    @DisplayName("복합 시나리오")
    inner class IntegrationScenarios {

        @Test
        fun `생성 후 활성화`() {
            // Given
            val category = createPositionCategory()
            val createdPosition = createPosition(
                id = 50L,
                name = "New Position",
                category = category,
                status = PositionStatus.CANDIDATE
            )

            every { positionCommand.save(any()) } returns createdPosition
            every { positionCommand.findById(50L) } returns createdPosition
            every { createdPosition.activate() } just Runs

            // When
            val created = service.create("New Position", category, null)
            service.activate(created.id)

            // Then
            verify(exactly = 1) { positionCommand.save(any()) }
            verify(exactly = 1) { createdPosition.activate() }
        }

        @Test
        fun `getOrCreate로 기존 Position 확인 후 활성화`() {
            // Given
            val normalizedName = "existing_position"
            val category = createPositionCategory()
            val existingPosition = createPosition(
                id = 60L,
                normalizedName = normalizedName,
                category = category,
                status = PositionStatus.DEPRECATED
            )

            every { positionCommand.findByNormalizedName(normalizedName) } returns existingPosition
            every { positionCommand.findById(60L) } returns existingPosition
            every { existingPosition.activate() } just Runs

            // When
            val position = service.getOrCreate("Existing Position", category, normalizedName)
            service.activate(position.id)

            // Then
            assertEquals(60L, position.id)
            verify(exactly = 1) { existingPosition.activate() }
            verify(exactly = 0) { positionCommand.save(any()) } // 새로 생성 안됨
        }

        @Test
        fun `활성화 후 비활성화`() {
            // Given
            val positionId = 70L
            val position = createPosition(
                id = positionId,
                status = PositionStatus.CANDIDATE
            )

            every { positionCommand.findById(positionId) } returns position
            every { position.activate() } just Runs
            every { position.deprecate() } just Runs

            // When
            service.activate(positionId)
            service.deprecate(positionId)

            // Then
            verify(exactly = 1) { position.activate() }
            verify(exactly = 1) { position.deprecate() }
        }

        @Test
        fun `같은 normalizedName으로 getOrCreate 여러 번 호출해도 동일한 Position 반환`() {
            // Given
            val normalizedName = "same_position"
            val category = createPositionCategory()
            val position = createPosition(
                id = 80L,
                normalizedName = normalizedName,
                category = category
            )

            every { positionCommand.findByNormalizedName(normalizedName) } returns position

            // When
            val first = service.getOrCreate("Same Position", category, normalizedName)
            val second = service.getOrCreate("Same Position", category, normalizedName)

            // Then
            assertEquals(first.id, second.id)
            verify(exactly = 2) { positionCommand.findByNormalizedName(normalizedName) }
            verify(exactly = 0) { positionCommand.save(any()) }
        }
    }
}