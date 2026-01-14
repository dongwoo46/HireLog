package com.hirelog.api.company.service


import com.hirelog.api.company.domain.Position
import com.hirelog.api.company.repository.PositionRepository
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service


@Service
class PositionService(
    private val positionRepository: PositionRepository
) {

    @Transactional
    fun getOrCreate(
        brandId: Long,
        positionName: String
    ): Position {

        val normalized = normalize(positionName)

        return positionRepository
            .findByBrandIdAndNormalizedName(brandId, normalized)
            ?: positionRepository.save(
                Position(
                    brandId = brandId,
                    name = positionName,
                    normalizedName = normalized
                )
            )
    }

    private fun normalize(value: String): String =
        value.lowercase()
            .replace(Regex("[^a-z0-9가-힣]"), "")
            .trim()
}
