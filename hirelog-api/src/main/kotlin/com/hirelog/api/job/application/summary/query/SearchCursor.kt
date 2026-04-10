package com.hirelog.api.job.application.summary.query

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.hirelog.api.common.exception.InvalidCursorException
import java.util.Base64

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes(
    JsonSubTypes.Type(value = SearchCursor.CreatedAt::class, name = "createdAt"),
    JsonSubTypes.Type(value = SearchCursor.Relevance::class, name = "relevance"),
    JsonSubTypes.Type(value = SearchCursor.Popular::class, name = "popular")
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

    data class Popular(
        val offset: Int
    ) : SearchCursor()

    companion object {
        private val mapper = jacksonObjectMapper()

        fun encode(cursor: SearchCursor): String {
            val json = mapper.writeValueAsString(cursor)
            return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(json.toByteArray(Charsets.UTF_8))
        }

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

