package com.hirelog.api.brand.infra.persistence.jpa

import com.hirelog.api.brand.domain.Brand
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface BrandJpaRepository : JpaRepository<Brand, Long> {

    fun findByNormalizedName(normalizedName: String): Brand?

    fun existsByNormalizedName(normalizedName: String): Boolean

    /**
     * Brand INSERT with ON CONFLICT DO NOTHING
     *
     * 동시성 안전한 Brand 생성:
     * - normalized_name 중복 시 아무 작업도 하지 않음
     * - 반환값: 영향 받은 row 수 (0 = 중복으로 insert 안 됨, 1 = 신규 생성)
     */
    @Modifying
    @Query(
        value = """
            INSERT INTO brand (name, normalized_name, company_id, verification_status, source, is_active, created_at, updated_at)
            VALUES (:name, :normalizedName, :companyId, :verificationStatus, :source, :isActive, now(), now())
            ON CONFLICT (normalized_name) DO NOTHING
        """,
        nativeQuery = true
    )
    fun insertIgnoreDuplicate(
        @Param("name") name: String,
        @Param("normalizedName") normalizedName: String,
        @Param("companyId") companyId: Long?,
        @Param("verificationStatus") verificationStatus: String,
        @Param("source") source: String,
        @Param("isActive") isActive: Boolean
    ): Int
}
