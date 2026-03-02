package com.hirelog.api.job.application.summary.query

import com.hirelog.api.common.exception.InvalidCursorException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.*
import java.util.Base64

@DisplayName("SearchCursor 테스트")
class SearchCursorTest {

    @Nested
    @DisplayName("CreatedAt 커서는")
    inner class CreatedAtCursorTest {

        @Test
        @DisplayName("encode 후 decode 하면 동일한 객체가 복원된다")
        fun shouldRoundTrip() {
            val original = SearchCursor.CreatedAt(createdAtMillis = 1709000000000L, id = 42L)

            val encoded = SearchCursor.encode(original)
            val decoded = SearchCursor.decode(encoded)

            assertThat(decoded).isInstanceOf(SearchCursor.CreatedAt::class.java)
            assertThat(decoded as SearchCursor.CreatedAt).isEqualTo(original)
        }

        @Test
        @DisplayName("encode 결과는 Base64 URL-safe 문자열이다 (패딩 문자 없음)")
        fun shouldProduceUrlSafeBase64WithoutPadding() {
            val cursor = SearchCursor.CreatedAt(createdAtMillis = 1709000000000L, id = 42L)
            val encoded = SearchCursor.encode(cursor)

            // URL-safe: '+', '/' 미포함 / 패딩 없음: '=' 미포함
            assertThat(encoded).doesNotContain("+", "/", "=")
        }

        @Test
        @DisplayName("type 필드가 'createdAt'으로 JSON에 포함된다")
        fun shouldIncludeTypeDiscriminator() {
            val cursor = SearchCursor.CreatedAt(createdAtMillis = 1709000000000L, id = 42L)
            val encoded = SearchCursor.encode(cursor)
            val json = String(Base64.getUrlDecoder().decode(encoded))

            assertThat(json).contains("\"type\":\"createdAt\"")
        }
    }

    @Nested
    @DisplayName("Relevance 커서는")
    inner class RelevanceCursorTest {

        @Test
        @DisplayName("encode 후 decode 하면 동일한 객체가 복원된다 (score 포함)")
        fun shouldRoundTrip() {
            val original = SearchCursor.Relevance(score = 1.234, createdAtMillis = 1709000000000L, id = 99L)

            val encoded = SearchCursor.encode(original)
            val decoded = SearchCursor.decode(encoded)

            assertThat(decoded).isInstanceOf(SearchCursor.Relevance::class.java)
            assertThat(decoded as SearchCursor.Relevance).isEqualTo(original)
        }

        @Test
        @DisplayName("type 필드가 'relevance'로 JSON에 포함된다")
        fun shouldIncludeTypeDiscriminator() {
            val cursor = SearchCursor.Relevance(score = 0.9, createdAtMillis = 1709000000000L, id = 1L)
            val encoded = SearchCursor.encode(cursor)
            val json = String(Base64.getUrlDecoder().decode(encoded))

            assertThat(json).contains("\"type\":\"relevance\"")
        }
    }

    @Nested
    @DisplayName("decode는")
    inner class DecodeTest {

        @Test
        @DisplayName("CreatedAt과 Relevance 커서를 타입별로 구별하여 복원한다")
        fun shouldDistinguishCursorTypes() {
            val createdAt = SearchCursor.encode(SearchCursor.CreatedAt(1709000000000L, 1L))
            val relevance = SearchCursor.encode(SearchCursor.Relevance(1.0, 1709000000000L, 1L))

            assertThat(SearchCursor.decode(createdAt)).isInstanceOf(SearchCursor.CreatedAt::class.java)
            assertThat(SearchCursor.decode(relevance)).isInstanceOf(SearchCursor.Relevance::class.java)
        }

        @Test
        @DisplayName("잘못된 Base64 문자열이면 InvalidCursorException을 던진다")
        fun shouldThrowOnInvalidBase64() {
            assertThatThrownBy {
                SearchCursor.decode("not-valid-base64!!!")
            }.isInstanceOf(InvalidCursorException::class.java)
        }

        @Test
        @DisplayName("유효한 Base64이지만 JSON 형식이 틀리면 InvalidCursorException을 던진다")
        fun shouldThrowOnMalformedJson() {
            val malformed = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{invalid-json}".toByteArray())

            assertThatThrownBy {
                SearchCursor.decode(malformed)
            }.isInstanceOf(InvalidCursorException::class.java)
        }

        @Test
        @DisplayName("알 수 없는 type 값이면 InvalidCursorException을 던진다")
        fun shouldThrowOnUnknownType() {
            val unknownType = """{"type":"unknown","createdAtMillis":1709000000000,"id":42}"""
            val encoded = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(unknownType.toByteArray())

            assertThatThrownBy {
                SearchCursor.decode(encoded)
            }.isInstanceOf(InvalidCursorException::class.java)
        }

        @Test
        @DisplayName("빈 문자열이면 InvalidCursorException을 던진다")
        fun shouldThrowOnEmptyString() {
            assertThatThrownBy {
                SearchCursor.decode("")
            }.isInstanceOf(InvalidCursorException::class.java)
        }
    }
}
