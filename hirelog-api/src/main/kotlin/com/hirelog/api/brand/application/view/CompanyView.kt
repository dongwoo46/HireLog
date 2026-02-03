package com.hirelog.api.brand.application.view

import com.hirelog.api.common.domain.VerificationStatus
import com.hirelog.api.company.domain.Company

/**
 * CompanyView
 *
 * 책임:
 * - Brand 상세 조회 시 포함되는 회사 정보 View
 * - 조회 전용 Projection 대상
 */
data class CompanyView(
    val id: Long,
    val name: String,
    val normalizedName: String,
    val verificationStatus: VerificationStatus,
    val isActive: Boolean
) {
    companion object {

        /**
         * Entity → View 변환
         *
         * 주의:
         * - QueryDSL projection 사용 시에는 직접 생성자를 사용
         * - 이 메서드는 Entity 기반 변환용
         */
        fun from(entity: Company): CompanyView =
            CompanyView(
                id = entity.id,
                name = entity.name,
                normalizedName = entity.normalizedName,
                verificationStatus = entity.verificationStatus,
                isActive = entity.isActive
            )
    }
}
