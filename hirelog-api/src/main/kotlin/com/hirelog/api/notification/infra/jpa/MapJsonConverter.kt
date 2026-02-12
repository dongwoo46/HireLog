package com.hirelog.api.notification.infra.jpa

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter
class MapJsonConverter : AttributeConverter<Map<String, Any?>, String> {

    companion object {
        private val mapper = jacksonObjectMapper()
    }

    override fun convertToDatabaseColumn(attribute: Map<String, Any?>?): String {
        return mapper.writeValueAsString(attribute ?: emptyMap<String, Any?>())
    }

    override fun convertToEntityAttribute(dbData: String?): Map<String, Any?> {
        if (dbData.isNullOrBlank()) return emptyMap()

        return try {
            mapper.readValue(
                dbData,
                mapper.typeFactory.constructMapType(
                    Map::class.java,
                    String::class.java,
                    Any::class.java
                )
            )
        } catch (e: Exception) {
            emptyMap()
        }
    }
}
