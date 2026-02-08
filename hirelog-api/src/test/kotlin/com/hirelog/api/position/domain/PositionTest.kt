package com.hirelog.api.position.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Position 도메인 테스트")
class PositionTest {

    private fun dummyCategory(): PositionCategory =
        PositionCategory.create(
            name = "Engineering",
            description = "엔지니어링 직군"
        )

    @Test
    @DisplayName("포지션 생성 시 초기 상태는 CANDIDATE여야 하며, 이름이 정규화되어야 한다")
    fun create() {
        // Arrange
        val name = "Backend Engineer"
        val description = "서버 개발자"
        val category = dummyCategory()

        // Act
        val position = Position.create(
            name = name,
            description = description,
            positionCategory = category
        )

        // Assert
        assertEquals(name, position.name)
        assertEquals("backend_engineer", position.normalizedName)
        assertEquals(PositionStatus.CANDIDATE, position.status)
        assertEquals(description, position.description)
        assertEquals(category, position.category)
    }

    @Test
    @DisplayName("특수문자와 공백이 포함된 이름도 올바르게 정규화되어야 한다")
    fun create_normalized() {
        // Arrange
        val name = "  C++ / Go  Developer!!  "
        val category = dummyCategory()

        // Act
        val position = Position.create(
            name = name,
            description = null,
            positionCategory = category
        )

        // Assert
        assertEquals("c_go_developer", position.normalizedName)
    }

    @Test
    @DisplayName("포지션 활성화 시 ACTIVE 상태로 변경되어야 한다")
    fun activate() {
        // Arrange
        val position = Position.create(
            name = "Test",
            description = null,
            positionCategory = dummyCategory()
        )

        // Act
        position.activate()

        // Assert
        assertEquals(PositionStatus.ACTIVE, position.status)
    }

    @Test
    @DisplayName("이미 ACTIVE 상태인 포지션을 활성화해도 상태는 ACTIVE여야 한다")
    fun activate_already_active() {
        // Arrange
        val position = Position.create(
            name = "Test",
            description = null,
            positionCategory = dummyCategory()
        )
        position.activate()

        // Act
        position.activate()

        // Assert
        assertEquals(PositionStatus.ACTIVE, position.status)
    }

    @Test
    @DisplayName("포지션 비활성화 시 DEPRECATED 상태로 변경되어야 한다")
    fun deprecate() {
        // Arrange
        val position = Position.create(
            name = "Test",
            description = null,
            positionCategory = dummyCategory()
        )
        position.activate()

        // Act
        position.deprecate()

        // Assert
        assertEquals(PositionStatus.DEPRECATED, position.status)
    }
}
