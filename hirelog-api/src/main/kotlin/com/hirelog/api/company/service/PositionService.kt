package com.hirelog.api.company.service


import com.hirelog.api.common.logging.log
import com.hirelog.api.common.normalize.Normalizer
import com.hirelog.api.company.domain.Position
import com.hirelog.api.company.repository.PositionRepository
import jakarta.transaction.Transactional
import org.springframework.dao.DataIntegrityViolationException
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

        val normalized = Normalizer.normalizeBrand(positionName)

        positionRepository
            .findByBrandIdAndNormalizedName(brandId, normalized)
            ?.let { return it }

        return try {
            positionRepository.save(
                Position(
                    brandId = brandId,
                    name = positionName,
                    normalizedName = normalized
                )
            )
        } catch (e: DataIntegrityViolationException) {
            log.debug(
                "Position already created concurrently. brandId={}, normalizedName={}",
                brandId,
                normalized
            )
            // 동시성으로 이미 생성된 경우
            positionRepository.findByBrandIdAndNormalizedName(brandId, normalized)
                ?: throw e
        }
    }
}
