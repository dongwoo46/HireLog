package com.hirelog.api.job.application.summary.query

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.hirelog.api.common.exception.InvalidCursorException
import java.util.Base64

/**
 * Search After 커서
 *
 * 정렬 방식별 커서 타입:
 * - CREATED_AT_DESC / CREATED_AT_ASC: CreatedAt (createdAtMillis, id)
 * - RELEVANCE: Relevance (score, createdAtMillis, id)
 *
 * 직렬화: JSON → Base64 URL-safe (패딩 없음)
 * type 필드로 Jackson 다형성 역직렬화
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes(
    JsonSubTypes.Type(value = SearchCursor.CreatedAt::class, name = "createdAt"),
    JsonSubTypes.Type(value = SearchCursor.Relevance::class, name = "relevance")
)
sealed class SearchCursor {

    data class CreatedAt(
        val createdAtMillis: Long,
        val id: Long
    ) : SearchCursor()

    data class Relevance(
        val score: Double,
        val createdAtMillis: Long,
        val id: Long
    ) : SearchCursor()

    companion object {
        private val mapper = jacksonObjectMapper()

        fun encode(cursor: SearchCursor): String {
            val json = mapper.writeValueAsString(cursor)
            return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(json.toByteArray(Charsets.UTF_8))
        }

        /**
         * cursor 디코딩
         *
         * 실패 시 null 반환이 아닌 InvalidCursorException 발생
         * - 클라이언트가 잘못된 cursor를 전달했음을 명시적으로 통보 (400)
         */
        fun decode(encoded: String): SearchCursor {
            return try {
                val json = Base64.getUrlDecoder().decode(encoded).toString(Charsets.UTF_8)
                mapper.readValue<SearchCursor>(json)
            } catch (e: Exception) {
                throw InvalidCursorException("Invalid cursor format: ${e.message}")
            }
        }
    }
}
