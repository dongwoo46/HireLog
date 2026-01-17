package com.hirelog.api.company.service

import com.hirelog.api.common.logging.log
import com.hirelog.api.common.normalize.Normalizer
import com.hirelog.api.company.domain.Brand
import com.hirelog.api.company.domain.BrandSource
import com.hirelog.api.company.domain.BrandVerificationStatus
import com.hirelog.api.company.repository.BrandRepository
import jakarta.transaction.Transactional
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service


@Service
class BrandService(
    private val brandRepository: BrandRepository
) {

    @Transactional
    fun getOrCreate(brandName: String): Brand {
        val normalized = Normalizer.normalizeBrand(brandName)

        brandRepository.findByNormalizedName(normalized)?.let {
            return it
        }

        return try {
            brandRepository.save(
                Brand(
                    name = brandName,
                    normalizedName = normalized,
                    companyId = null,
                    verificationStatus = BrandVerificationStatus.UNVERIFIED,
                    source = BrandSource.USER
                )
            )
        } catch (e: DataIntegrityViolationException) {
            log.debug(
                "Brand already created concurrently. normalizedName={}",
                normalized
            )
            // 동시성으로 누군가 먼저 INSERT 한 경우
            brandRepository.findByNormalizedName(normalized)
                ?: throw e
        }
    }
}
