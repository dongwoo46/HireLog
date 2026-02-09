package com.hirelog.api.common.domain.process

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("ProcessedEventId 도메인 테스트")
class ProcessedEventIdTest {

    @Nested
    @DisplayName("create 메서드는")
    inner class CreateTest {

        @Test
        @DisplayName("문자열로 ProcessedEventId를 생성한다")
        fun shouldCreateProcessedEventId() {
            // given
            val value = "MEMBER:123:MEMBER_CREATED"

            // when
            val eventId = ProcessedEventId.create(value)

            // then
            assertThat(eventId.value).isEqualTo(value)
        }

        @Test
        @DisplayName("다양한 형식의 문자열로 생성할 수 있다")
        fun shouldCreateWithVariousFormats() {
            // given
            val values = listOf(
                "simple-id",
                "AGGREGATE:ID:EVENT",
                "uuid-format-12345",
                "service.event.123"
            )

            // when & then
            values.forEach { value ->
                val eventId = ProcessedEventId.create(value)
                assertThat(eventId.value).isEqualTo(value)
            }
        }

        @Test
        @DisplayName("빈 문자열로는 생성할 수 없다")
        fun shouldNotCreateWithBlankValue() {
            // when & then
            assertThatThrownBy {
                ProcessedEventId.create("")
            }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("ProcessedEventId must not be blank")
        }

        @Test
        @DisplayName("공백 문자열로는 생성할 수 없다")
        fun shouldNotCreateWithWhitespaceValue() {
            // when & then
            assertThatThrownBy {
                ProcessedEventId.create("   ")
            }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("ProcessedEventId must not be blank")
        }
    }

    @Nested
    @DisplayName("equals 메서드는")
    inner class EqualsTest {

        @Test
        @DisplayName("동일한 value를 가진 객체는 동등하다")
        fun shouldBeEqualWithSameValue() {
            // given
            val value = "MEMBER:123:CREATED"
            val eventId1 = ProcessedEventId.create(value)
            val eventId2 = ProcessedEventId.create(value)

            // when & then
            assertThat(eventId1).isEqualTo(eventId2)
            assertThat(eventId1 == eventId2).isTrue()
        }

        @Test
        @DisplayName("다른 value를 가진 객체는 동등하지 않다")
        fun shouldNotBeEqualWithDifferentValue() {
            // given
            val eventId1 = ProcessedEventId.create("MEMBER:123:CREATED")
            val eventId2 = ProcessedEventId.create("MEMBER:456:CREATED")

            // when & then
            assertThat(eventId1).isNotEqualTo(eventId2)
            assertThat(eventId1 == eventId2).isFalse()
        }

        @Test
        @DisplayName("자기 자신과는 동등하다")
        fun shouldBeEqualToItself() {
            // given
            val eventId = ProcessedEventId.create("MEMBER:123:CREATED")

            // when & then
            assertThat(eventId).isEqualTo(eventId)
            assertThat(eventId == eventId).isTrue()
        }

        @Test
        @DisplayName("null과는 동등하지 않다")
        fun shouldNotBeEqualToNull() {
            // given
            val eventId = ProcessedEventId.create("MEMBER:123:CREATED")

            // when & then
            assertThat(eventId).isNotEqualTo(null)
        }

        @Test
        @DisplayName("다른 타입의 객체와는 동등하지 않다")
        fun shouldNotBeEqualToDifferentType() {
            // given
            val eventId = ProcessedEventId.create("MEMBER:123:CREATED")
            val string = "MEMBER:123:CREATED"

            // when & then
            assertThat(eventId).isNotEqualTo(string)
        }
    }

    @Nested
    @DisplayName("hashCode 메서드는")
    inner class HashCodeTest {

        @Test
        @DisplayName("동일한 value를 가진 객체는 동일한 hashCode를 반환한다")
        fun shouldHaveSameHashCodeWithSameValue() {
            // given
            val value = "MEMBER:123:CREATED"
            val eventId1 = ProcessedEventId.create(value)
            val eventId2 = ProcessedEventId.create(value)

            // when & then
            assertThat(eventId1.hashCode()).isEqualTo(eventId2.hashCode())
        }

        @Test
        @DisplayName("Set과 Map에서 키로 사용할 수 있다")
        fun shouldBeUsableAsKeyInCollections() {
            // given
            val eventId1 = ProcessedEventId.create("MEMBER:123:CREATED")
            val eventId2 = ProcessedEventId.create("MEMBER:123:CREATED")
            val eventId3 = ProcessedEventId.create("MEMBER:456:CREATED")

            val set = mutableSetOf<ProcessedEventId>()
            val map = mutableMapOf<ProcessedEventId, String>()

            // when
            set.add(eventId1)
            set.add(eventId2) // 동일한 값이므로 추가되지 않음
            set.add(eventId3)

            map[eventId1] = "value1"
            map[eventId2] = "value2" // 동일한 키이므로 덮어씀

            // then
            assertThat(set).hasSize(2)
            assertThat(map).hasSize(1)
            assertThat(map[eventId1]).isEqualTo("value2")
        }
    }

    @Nested
    @DisplayName("toString 메서드는")
    inner class ToStringTest {

        @Test
        @DisplayName("value를 반환한다")
        fun shouldReturnValue() {
            // given
            val value = "MEMBER:123:MEMBER_CREATED"
            val eventId = ProcessedEventId.create(value)

            // when
            val result = eventId.toString()

            // then
            assertThat(result).isEqualTo(value)
        }

        @Test
        @DisplayName("다양한 형식의 value를 문자열로 반환한다")
        fun shouldReturnStringForVariousFormats() {
            // given
            val values = listOf(
                "simple-id",
                "BRAND:456:VERIFIED",
                "uuid-12345-67890",
                "event.type.identifier"
            )

            // when & then
            values.forEach { value ->
                val eventId = ProcessedEventId.create(value)
                assertThat(eventId.toString()).isEqualTo(value)
            }
        }
    }

    @Nested
    @DisplayName("값 객체로서의 불변성")
    inner class ImmutabilityTest {

        @Test
        @DisplayName("생성 후 value는 변경되지 않는다")
        fun shouldBeImmutable() {
            // given
            val originalValue = "MEMBER:123:CREATED"
            val eventId = ProcessedEventId.create(originalValue)

            // when
            val retrievedValue = eventId.value

            // then
            assertThat(retrievedValue).isEqualTo(originalValue)
            assertThat(eventId.value).isEqualTo(originalValue) // 여전히 동일
        }
    }
}