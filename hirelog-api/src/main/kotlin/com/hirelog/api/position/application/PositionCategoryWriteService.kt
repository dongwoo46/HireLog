package com.hirelog.api.position.application

import com.hirelog.api.position.application.port.PositionCategoryCommand
import com.hirelog.api.position.application.port.PositionCategoryQuery
import com.hirelog.api.position.domain.PositionCategory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class PositionCategoryWriteService(
    private val positionCategoryCommand: PositionCategoryCommand,
    private val positionCategoryQuery: PositionCategoryQuery
) {

    fun createIfAbsent(name: String, description: String? = null): PositionCategory {
        val normalized = normalize(name)

        return positionCategoryQuery.findByNormalizedName(normalized)
            ?: positionCategoryCommand.save(PositionCategory.create(name, description))
    }

    private fun normalize(value: String): String =
        value.lowercase()
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
}
