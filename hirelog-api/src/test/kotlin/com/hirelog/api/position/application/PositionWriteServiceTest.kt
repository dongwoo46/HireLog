package com.hirelog.api.position.application

import com.hirelog.api.common.exception.EntityAlreadyExistsException
import com.hirelog.api.common.exception.EntityNotFoundException
import com.hirelog.api.position.application.port.PositionCategoryCommand
import com.hirelog.api.position.application.port.PositionCommand
import com.hirelog.api.position.domain.Position
import com.hirelog.api.position.domain.PositionCategory
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.dao.DataIntegrityViolationException

@DisplayName("PositionWriteService")
class PositionWriteServiceTest {

    private lateinit var positionCommand: PositionCommand
    private lateinit var positionCategoryCommand: PositionCategoryCommand
    private lateinit var positionWriteService: PositionWriteService

    @BeforeEach
    fun setUp() {
        positionCommand = mockk()
        positionCategoryCommand = mockk()
        positionWriteService = PositionWriteService(positionCommand, positionCategoryCommand)
    }

    @Nested
    @DisplayName("create()")
    inner class Create {

        @Test
        @DisplayName("카테고리가 존재하면 포지션을 생성하고 저장된 결과를 반환한다")
        fun `creates position when category exists`() {
            // given
            val name = "백엔드 개발자"
            val categoryId = 1L
            val description = "서버 개발"

            val category = mockk<PositionCategory>()
            val saved = mockk<Position>()

            every { positionCategoryCommand.findById(categoryId) } returns category
            every { positionCommand.save(any()) } returns saved

            // when
            val result = positionWriteService.create(name, categoryId, description)

            // then
            assertThat(result).isEqualTo(saved)
            verify(exactly = 1) { positionCommand.save(any()) }
        }

        @Test
        @DisplayName("description이 null이어도 생성할 수 있다")
        fun `creates position without description`() {
            // given
            val category = mockk<PositionCategory>()
            val saved = mockk<Position>()

            every { positionCategoryCommand.findById(any()) } returns category
            every { positionCommand.save(any()) } returns saved

            // when
            val result = positionWriteService.create("프론트엔드", 1L, null)

            // then
            assertThat(result).isEqualTo(saved)
        }

        @Test
        @DisplayName("카테고리가 존재하지 않으면 EntityNotFoundException을 던진다")
        fun `throws EntityNotFoundException when category not found`() {
            // given
            val categoryId = 999L

            every { positionCategoryCommand.findById(categoryId) } returns null

            // when & then
            assertThatThrownBy {
                positionWriteService.create("백엔드", categoryId, null)
            }.isInstanceOf(EntityNotFoundException::class.java)

            verify(exactly = 0) { positionCommand.save(any()) }
        }

        @Test
        @DisplayName("normalizedName 중복이면 EntityAlreadyExistsException을 던진다")
        fun `throws EntityAlreadyExistsException on duplicate normalizedName`() {
            // given
            val category = mockk<PositionCategory>()
            val position = mockk<Position> {
                every { normalizedName } returns "backend"
            }

            every { positionCategoryCommand.findById(any()) } returns category
            every { positionCommand.save(any()) } throws DataIntegrityViolationException("duplicate")

            // when & then
            assertThatThrownBy {
                positionWriteService.create("백엔드", 1L, null)
            }.isInstanceOf(EntityAlreadyExistsException::class.java)
        }
    }

    @Nested
    @DisplayName("activate()")
    inner class Activate {

        @Test
        @DisplayName("포지션을 활성화하고 저장한다")
        fun `activates and saves position`() {
            // given
            val positionId = 1L
            val position = mockk<Position>(relaxed = true)

            every { positionCommand.findById(positionId) } returns position
            every { positionCommand.save(position) } returns position

            // when
            positionWriteService.activate(positionId)

            // then
            verify(exactly = 1) { position.activate() }
            verify(exactly = 1) { positionCommand.save(position) }
        }

        @Test
        @DisplayName("존재하지 않는 positionId면 EntityNotFoundException을 던진다")
        fun `throws EntityNotFoundException when position not found`() {
            // given
            every { positionCommand.findById(any()) } returns null

            // when & then
            assertThatThrownBy {
                positionWriteService.activate(999L)
            }.isInstanceOf(EntityNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("deprecate()")
    inner class Deprecate {

        @Test
        @DisplayName("포지션을 비활성화하고 저장한다")
        fun `deprecates and saves position`() {
            // given
            val positionId = 2L
            val position = mockk<Position>(relaxed = true)

            every { positionCommand.findById(positionId) } returns position
            every { positionCommand.save(position) } returns position

            // when
            positionWriteService.deprecate(positionId)

            // then
            verify(exactly = 1) { position.deprecate() }
            verify(exactly = 1) { positionCommand.save(position) }
        }

        @Test
        @DisplayName("존재하지 않는 positionId면 EntityNotFoundException을 던진다")
        fun `throws EntityNotFoundException when position not found`() {
            // given
            every { positionCommand.findById(any()) } returns null

            // when & then
            assertThatThrownBy {
                positionWriteService.deprecate(999L)
            }.isInstanceOf(EntityNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("getOrCreate()")
    inner class GetOrCreate {

        @Test
        @DisplayName("이미 존재하는 포지션이 있으면 저장하지 않고 반환한다")
        fun `returns existing position without saving`() {
            // given
            val name = "iOS 개발자"
            val category = mockk<PositionCategory>()
            val existing = mockk<Position>()

            every { positionCommand.findByNormalizedName(any()) } returns existing

            // when
            val result = positionWriteService.getOrCreate(name, category)

            // then
            assertThat(result).isEqualTo(existing)
            verify(exactly = 0) { positionCommand.save(any()) }
        }

        @Test
        @DisplayName("존재하지 않으면 새로 생성하고 저장한 뒤 반환한다")
        fun `creates and saves new position when not found`() {
            // given
            val name = "Android 개발자"
            val category = mockk<PositionCategory>()
            val saved = mockk<Position>()

            every { positionCommand.findByNormalizedName(any()) } returns null
            every { positionCommand.save(any()) } returns saved

            // when
            val result = positionWriteService.getOrCreate(name, category)

            // then
            assertThat(result).isEqualTo(saved)
            verify(exactly = 1) { positionCommand.save(any()) }
        }

        @Test
        @DisplayName("저장 중 DataIntegrityViolationException 발생 시 재조회 후 반환한다 (동시성 경쟁)")
        fun `returns position found after DataIntegrityViolationException on save`() {
            // given
            val name = "DevOps"
            val category = mockk<PositionCategory>()
            val raceWinner = mockk<Position>()

            every { positionCommand.findByNormalizedName(any()) } returnsMany listOf(
                null,        // 첫 번째 조회: 없음
                raceWinner   // 예외 후 재조회: 다른 트랜잭션이 먼저 저장
            )
            every { positionCommand.save(any()) } throws DataIntegrityViolationException("duplicate")

            // when
            val result = positionWriteService.getOrCreate(name, category)

            // then
            assertThat(result).isEqualTo(raceWinner)
            verify(exactly = 2) { positionCommand.findByNormalizedName(any()) }
        }

        @Test
        @DisplayName("저장 예외 후 재조회도 실패하면 DataIntegrityViolationException을 그대로 던진다")
        fun `rethrows DataIntegrityViolationException when retry also fails`() {
            // given
            val name = "DataEngineer"
            val category = mockk<PositionCategory>()

            every { positionCommand.findByNormalizedName(any()) } returns null
            every { positionCommand.save(any()) } throws DataIntegrityViolationException("duplicate")

            // when & then
            assertThatThrownBy {
                positionWriteService.getOrCreate(name, category)
            }.isInstanceOf(DataIntegrityViolationException::class.java)
        }
    }
}