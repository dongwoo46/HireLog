package com.hirelog.api.position.domain

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("PositionCategory")
class PositionCategoryTest {

    @Nested
    @DisplayName("create()")
    inner class Create {

        @Test
        @DisplayName("name, description으로 PositionCategory를 생성한다")
        fun `creates with name and description`() {
            // when
            val category = PositionCategory.create("IT / Software", "IT 직군")

            // then
            assertThat(category.name).isEqualTo("IT / Software")
            assertThat(category.description).isEqualTo("IT 직군")
        }

        @Test
        @DisplayName("생성 시 상태는 ACTIVE이다")
        fun `initial status is ACTIVE`() {
            // when
            val category = PositionCategory.create("Marketing", null)

            // then
            assertThat(category.status).isEqualTo(PositionStatus.ACTIVE)
        }

        @Test
        @DisplayName("description 없이도 생성할 수 있다")
        fun `creates without description`() {
            // when
            val category = PositionCategory.create("Design", null)

            // then
            assertThat(category.description).isNull()
        }

        @Test
        @DisplayName("normalizedName은 소문자 + 공백을 underscore로 변환한다")
        fun `normalizedName is lowercased and space replaced with underscore`() {
            // when
            val category = PositionCategory.create("IT Software", null)

            // then
            assertThat(category.normalizedName).isEqualTo("it_software")
        }

        @Test
        @DisplayName("normalizedName은 특수문자를 제거한다")
        fun `normalizedName removes special characters`() {
            // when
            val category = PositionCategory.create("IT / Software", null)

            // then
            assertThat(category.normalizedName).doesNotContain("/", "-", ".")
        }

        @Test
        @DisplayName("동일한 의미의 이름은 같은 normalizedName을 가진다")
        fun `same meaning names produce same normalizedName`() {
            // when
            val category1 = PositionCategory.create("IT Software", null)
            val category2 = PositionCategory.create("it software", null)

            // then
            assertThat(category1.normalizedName).isEqualTo(category2.normalizedName)
        }
    }

    @Nested
    @DisplayName("activate()")
    inner class Activate {

        @Test
        @DisplayName("INACTIVE 상태에서 activate() 호출 시 ACTIVE로 전환된다")
        fun `changes status from INACTIVE to ACTIVE`() {
            // given
            val category = PositionCategory.create("Marketing", null)
            category.deactivate() // INACTIVE로 전환
            assertThat(category.status).isEqualTo(PositionStatus.INACTIVE)

            // when
            category.activate()

            // then
            assertThat(category.status).isEqualTo(PositionStatus.ACTIVE)
        }

        @Test
        @DisplayName("ACTIVE 상태에서 activate() 호출 시 IllegalArgumentException을 던진다")
        fun `throws IllegalArgumentException when already ACTIVE`() {
            // given
            val category = PositionCategory.create("Marketing", null)
            assertThat(category.status).isEqualTo(PositionStatus.ACTIVE)

            // when & then
            assertThatThrownBy {
                category.activate()
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("INACTIVE")
        }
    }

    @Nested
    @DisplayName("deactivate()")
    inner class Deactivate {

        @Test
        @DisplayName("ACTIVE 상태에서 deactivate() 호출 시 INACTIVE로 전환된다")
        fun `changes status from ACTIVE to INACTIVE`() {
            // given
            val category = PositionCategory.create("Design", null)
            assertThat(category.status).isEqualTo(PositionStatus.ACTIVE)

            // when
            category.deactivate()

            // then
            assertThat(category.status).isEqualTo(PositionStatus.INACTIVE)
        }

        @Test
        @DisplayName("INACTIVE 상태에서 deactivate() 호출 시 IllegalArgumentException을 던진다")
        fun `throws IllegalArgumentException when already INACTIVE`() {
            // given
            val category = PositionCategory.create("Design", null)
            category.deactivate()
            assertThat(category.status).isEqualTo(PositionStatus.INACTIVE)

            // when & then
            assertThatThrownBy {
                category.deactivate()
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("ACTIVE")
        }
    }

    @Nested
    @DisplayName("상태 전이 시나리오")
    inner class StatusTransition {

        @Test
        @DisplayName("ACTIVE → INACTIVE → ACTIVE 전환이 정상 동작한다")
        fun `can cycle between ACTIVE and INACTIVE`() {
            // given
            val category = PositionCategory.create("Finance", null)

            // when & then
            assertThat(category.status).isEqualTo(PositionStatus.ACTIVE)

            category.deactivate()
            assertThat(category.status).isEqualTo(PositionStatus.INACTIVE)

            category.activate()
            assertThat(category.status).isEqualTo(PositionStatus.ACTIVE)
        }
    }
}