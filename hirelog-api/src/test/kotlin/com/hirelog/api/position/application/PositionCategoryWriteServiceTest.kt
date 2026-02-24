package com.hirelog.api.position.application

import com.hirelog.api.common.exception.EntityAlreadyExistsException
import com.hirelog.api.common.exception.EntityNotFoundException
import com.hirelog.api.position.application.port.PositionCategoryCommand
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

@DisplayName("PositionCategoryWriteService")
class PositionCategoryWriteServiceTest {

    private lateinit var positionCategoryCommand: PositionCategoryCommand
    private lateinit var positionCategoryWriteService: PositionCategoryWriteService

    @BeforeEach
    fun setUp() {
        positionCategoryCommand = mockk()
        positionCategoryWriteService = PositionCategoryWriteService(positionCategoryCommand)
    }

    @Nested
    @DisplayName("getOrCreate()")
    inner class GetOrCreate {

        @Test
        @DisplayName("이미 존재하는 카테고리가 있으면 저장하지 않고 기존 것을 반환한다")
        fun `returns existing category without saving`() {
            // given
            val name = "백엔드"
            val existing = mockk<PositionCategory>()

            every { positionCategoryCommand.findByNormalizedName(any()) } returns existing

            // when
            val result = positionCategoryWriteService.getOrCreate(name)

            // then
            assertThat(result).isEqualTo(existing)
            verify(exactly = 0) { positionCategoryCommand.save(any()) }
        }

        @Test
        @DisplayName("존재하지 않으면 새로 생성하고 저장한 뒤 반환한다")
        fun `creates and saves new category when not found`() {
            // given
            val name = "프론트엔드"
            val saved = mockk<PositionCategory>()

            every { positionCategoryCommand.findByNormalizedName(any()) } returns null
            every { positionCategoryCommand.save(any()) } returns saved

            // when
            val result = positionCategoryWriteService.getOrCreate(name)

            // then
            assertThat(result).isEqualTo(saved)
            verify(exactly = 1) { positionCategoryCommand.save(any()) }
        }

        @Test
        @DisplayName("저장 중 DataIntegrityViolationException 발생 시 재조회 후 반환한다 (동시성 경쟁)")
        fun `returns category found after DataIntegrityViolationException on save`() {
            // given
            val name = "DevOps"
            val raceWinner = mockk<PositionCategory>()

            every { positionCategoryCommand.findByNormalizedName(any()) } returnsMany listOf(
                null,         // 첫 번째 조회: 없음
                raceWinner    // 예외 후 재조회: 다른 트랜잭션이 먼저 저장함
            )
            every { positionCategoryCommand.save(any()) } throws DataIntegrityViolationException("duplicate")

            // when
            val result = positionCategoryWriteService.getOrCreate(name)

            // then
            assertThat(result).isEqualTo(raceWinner)
            verify(exactly = 2) { positionCategoryCommand.findByNormalizedName(any()) }
        }

        @Test
        @DisplayName("저장 중 DataIntegrityViolationException 발생 후 재조회도 실패하면 예외를 그대로 던진다")
        fun `rethrows DataIntegrityViolationException when retry also fails`() {
            // given
            val name = "DataEngineer"
            val originalEx = DataIntegrityViolationException("duplicate")

            every { positionCategoryCommand.findByNormalizedName(any()) } returns null
            every { positionCategoryCommand.save(any()) } throws originalEx

            // when & then
            assertThatThrownBy {
                positionCategoryWriteService.getOrCreate(name)
            }.isInstanceOf(DataIntegrityViolationException::class.java)
        }
    }

    @Nested
    @DisplayName("create()")
    inner class Create {

        @Test
        @DisplayName("새 카테고리를 생성하고 저장된 결과를 반환한다")
        fun `creates and returns new category`() {
            // given
            val name = "iOS"
            val description = "iOS 개발"
            val saved = mockk<PositionCategory>()

            every { positionCategoryCommand.save(any()) } returns saved

            // when
            val result = positionCategoryWriteService.create(name, description)

            // then
            assertThat(result).isEqualTo(saved)
            verify(exactly = 1) { positionCategoryCommand.save(any()) }
        }

        @Test
        @DisplayName("description 없이도 생성할 수 있다")
        fun `creates category without description`() {
            // given
            val saved = mockk<PositionCategory>()
            every { positionCategoryCommand.save(any()) } returns saved

            // when
            val result = positionCategoryWriteService.create("Android", null)

            // then
            assertThat(result).isEqualTo(saved)
        }

        @Test
        @DisplayName("중복 이름이면 EntityAlreadyExistsException을 던진다")
        fun `throws EntityAlreadyExistsException on duplicate name`() {
            // given
            val name = "백엔드"

            every { positionCategoryCommand.save(any()) } throws DataIntegrityViolationException("duplicate")

            // when & then
            assertThatThrownBy {
                positionCategoryWriteService.create(name, null)
            }.isInstanceOf(EntityAlreadyExistsException::class.java)
                .hasMessageContaining(name)
        }
    }

    @Nested
    @DisplayName("activate()")
    inner class Activate {

        @Test
        @DisplayName("카테고리를 활성화하고 저장한다")
        fun `activates and saves category`() {
            // given
            val categoryId = 1L
            val category = mockk<PositionCategory>(relaxed = true)

            every { positionCategoryCommand.findById(categoryId) } returns category
            every { positionCategoryCommand.save(category) } returns category

            // when
            positionCategoryWriteService.activate(categoryId)

            // then
            verify(exactly = 1) { category.activate() }
            verify(exactly = 1) { positionCategoryCommand.save(category) }
        }

        @Test
        @DisplayName("존재하지 않는 categoryId면 EntityNotFoundException을 던진다")
        fun `throws EntityNotFoundException when category not found`() {
            // given
            val categoryId = 999L

            every { positionCategoryCommand.findById(categoryId) } returns null

            // when & then
            assertThatThrownBy {
                positionCategoryWriteService.activate(categoryId)
            }.isInstanceOf(EntityNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("deactivate()")
    inner class Deactivate {

        @Test
        @DisplayName("카테고리를 비활성화하고 저장한다")
        fun `deactivates and saves category`() {
            // given
            val categoryId = 2L
            val category = mockk<PositionCategory>(relaxed = true)

            every { positionCategoryCommand.findById(categoryId) } returns category
            every { positionCategoryCommand.save(category) } returns category

            // when
            positionCategoryWriteService.deactivate(categoryId)

            // then
            verify(exactly = 1) { category.deactivate() }
            verify(exactly = 1) { positionCategoryCommand.save(category) }
        }

        @Test
        @DisplayName("존재하지 않는 categoryId면 EntityNotFoundException을 던진다")
        fun `throws EntityNotFoundException when category not found`() {
            // given
            val categoryId = 999L

            every { positionCategoryCommand.findById(categoryId) } returns null

            // when & then
            assertThatThrownBy {
                positionCategoryWriteService.deactivate(categoryId)
            }.isInstanceOf(EntityNotFoundException::class.java)
        }
    }
}