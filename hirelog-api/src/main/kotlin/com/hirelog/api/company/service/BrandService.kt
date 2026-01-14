package com.hirelog.api.company.service

import com.hirelog.api.company.domain.Brand
import com.hirelog.api.company.domain.BrandSource
import com.hirelog.api.company.domain.BrandVerificationStatus
import com.hirelog.api.company.repository.BrandRepository
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service


@Service
class BrandService(
    private val brandRepository: BrandRepository
) {

    @Transactional
    fun getOrCreate(brandName: String): Brand {
        val normalized = normalize(brandName)

        return brandRepository.findByNormalizedName(normalized)
            ?: brandRepository.save(
                Brand(
                    name = brandName,
                    normalizedName = normalized,
                    companyId = null,
                    verificationStatus = BrandVerificationStatus.UNVERIFIED,
                    source = BrandSource.USER
                )
            )
    }

    private fun normalize(value: String): String =
        value.lowercase()
            .replace(Regex("[^a-z0-9가-힣]"), "")
            .trim()
}
