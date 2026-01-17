package com.hirelog.api.common.jpa

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter
class StringListJsonConverter : AttributeConverter<List<String>, String> {

    companion object {
        private val mapper = jacksonObjectMapper()
    }

    override fun convertToDatabaseColumn(attribute: List<String>?): String {
        return mapper.writeValueAsString(attribute ?: emptyList<String>())
    }

    override fun convertToEntityAttribute(dbData: String?): List<String> {
        if (dbData.isNullOrBlank()) return emptyList()

        return try {
            mapper.readValue(
                dbData,
                mapper.typeFactory.constructCollectionType(
                    List::class.java,
                    String::class.java
                )
            )
        } catch (e: Exception) {
            // 로그만 남기고 안전하게 복구
            emptyList()
        }
    }
}
