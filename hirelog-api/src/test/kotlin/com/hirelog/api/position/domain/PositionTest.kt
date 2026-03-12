package com.hirelog.api.position.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("Position")
class PositionTest {

    private lateinit var category: PositionCategory

    @BeforeEach
    fun setUp() {
        category = PositionCategory.create("개발", null)
    }

    @Nested
    @DisplayName("create()")
    inner class Create {

        @Test
        @DisplayName("name, category, description으로 Position을 생성한다")
        fun `creates position with all fields`() {
            // when
            val position = Position.create(
                name = "Backend Engineer",
                positionCategory = category,
                description = "서버 개발"
            )

            // then
            assertThat(position.name).isEqualTo("Backend Engineer")
            assertThat(position.description).isEqualTo("서버 개발")
            assertThat(position.category).isEqualTo(category)
        }

        @Test
        @DisplayName("생성 시 상태는 ACTIVE이다")
        fun `initial status is ACTIVE`() {
            // when
            val position = Position.create(
                name = "Backend Engineer",
                positionCategory = category,
                description = null
            )

            // then
            assertThat(position.status).isEqualTo(PositionStatus.ACTIVE)
        }

        @Test
        @DisplayName("normalizedName은 소문자 + 공백을 underscore로 변환한다")
        fun `normalizedName is lowercased and space replaced with underscore`() {
            // when
            val position = Position.create(
                name = "Backend Engineer",
                positionCategory = category,
                description = null
            )

            // then
            assertThat(position.normalizedName).isEqualTo("backend_engineer")
        }

        @Test
        @DisplayName("normalizedName은 특수문자를 제거한다")
        fun `normalizedName removes special characters`() {
            // when
            val position = Position.create(
                name = "iOS Developer",
                positionCategory = category,
                description = null
            )

            // then
            assertThat(position.normalizedName).doesNotContain("/", "-", ".")
        }

        @Test
        @DisplayName("description 없이도 생성할 수 있다")
        fun `creates position without description`() {
            // when
            val position = Position.create(
                name = "Data Engineer",
                positionCategory = category,
                description = null
            )

            // then
            assertThat(position.description).isNull()
        }
    }

    @Nested
    @DisplayName("activate()")
    inner class Activate {

        @Test
        @DisplayName("INACTIVE 상태에서 activate() 호출 시 ACTIVE로 전환된다")
        fun `changes status from INACTIVE to ACTIVE`() {
            // given
            val position = Position.create("Backend Engineer", category, null)
            position.deprecate() // INACTIVE로 먼저 전환
            assertThat(position.status).isEqualTo(PositionStatus.INACTIVE)

            // when
            position.activate()

            // then
            assertThat(position.status).isEqualTo(PositionStatus.ACTIVE)
        }

        @Test
        @DisplayName("이미 ACTIVE 상태에서 activate() 호출 시 상태가 유지된다 (idempotent)")
        fun `does nothing when already ACTIVE`() {
            // given
            val position = Position.create("Backend Engineer", category, null)
            assertThat(position.status).isEqualTo(PositionStatus.ACTIVE)

            // when
            position.activate()

            // then
            assertThat(position.status).isEqualTo(PositionStatus.ACTIVE)
        }
    }

    @Nested
    @DisplayName("deprecate()")
    inner class Deprecate {

        @Test
        @DisplayName("ACTIVE 상태에서 deprecate() 호출 시 INACTIVE로 전환된다")
        fun `changes status from ACTIVE to INACTIVE`() {
            // given
            val position = Position.create("Backend Engineer", category, null)
            assertThat(position.status).isEqualTo(PositionStatus.ACTIVE)

            // when
            position.deprecate()

            // then
            assertThat(position.status).isEqualTo(PositionStatus.INACTIVE)
        }

        @Test
        @DisplayName("이미 INACTIVE 상태에서 deprecate() 호출 시 상태가 유지된다 (idempotent)")
        fun `does nothing when already INACTIVE`() {
            // given
            val position = Position.create("Backend Engineer", category, null)
            position.deprecate()
            assertThat(position.status).isEqualTo(PositionStatus.INACTIVE)

            // when
            position.deprecate()

            // then
            assertThat(position.status).isEqualTo(PositionStatus.INACTIVE)
        }
    }

    @Nested
    @DisplayName("normalizedName 정규화 규칙")
    inner class NormalizedName {

        @Test
        @DisplayName("대문자를 소문자로 변환한다")
        fun `converts uppercase to lowercase`() {
            val position = Position.create("BACKEND ENGINEER", category, null)
            assertThat(position.normalizedName).isEqualTo(position.normalizedName.lowercase())
        }

        @Test
        @DisplayName("동일한 의미의 이름은 같은 normalizedName을 가진다")
        fun `same meaning names produce same normalizedName`() {
            val position1 = Position.create("Backend Engineer", category, null)
            val position2 = Position.create("backend engineer", category, null)

            assertThat(position1.normalizedName).isEqualTo(position2.normalizedName)
        }
    }
}