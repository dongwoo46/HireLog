package com.hirelog.api.common.jpa

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter
class StringListJsonConverter : AttributeConverter<List<String>, String> {

    private val mapper = jacksonObjectMapper()

    override fun convertToDatabaseColumn(attribute: List<String>?): String {
        return mapper.writeValueAsString(attribute ?: emptyList<String>())
    }

    override fun convertToEntityAttribute(dbData: String?): List<String> {
        return if (dbData.isNullOrBlank()) {
            emptyList()
        } else {
            mapper.readValue(
                dbData,
                mapper.typeFactory.constructCollectionType(List::class.java, String::class.java)
            )
        }
    }
}
