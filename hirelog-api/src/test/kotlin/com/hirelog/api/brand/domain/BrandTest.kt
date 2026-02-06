package com.hirelog.api.brand.domain

import com.hirelog.api.common.domain.VerificationStatus
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Brand 도메인 테스트")
class BrandTest {

    @Test
    @DisplayName("브랜드 생성 시 초기 상태는 UNVERIFIED, isActive=true여야 한다")
    fun create() {
        // Arrange
        val name = "Toss"
        val normalizedName = "toss"
        val companyId = 1L
        val source = BrandSource.ADMIN

        // Act
        val brand = Brand.create(name, normalizedName, companyId, source)

        // Assert
        assertEquals(name, brand.name)
        assertEquals(normalizedName, brand.normalizedName)
        assertEquals(companyId, brand.companyId)
        assertEquals(source, brand.source)
        assertEquals(VerificationStatus.UNVERIFIED, brand.verificationStatus)
        assertTrue(brand.isActive)
    }

    @Test
    @DisplayName("브랜드 검증 승인: 상태가 VERIFIED로 변경되어야 한다")
    fun verify() {
        // Arrange
        val brand = Brand.create("Toss", "toss", 1L, BrandSource.ADMIN)

        // Act
        brand.verify()

        // Assert
        assertEquals(VerificationStatus.VERIFIED, brand.verificationStatus)
    }

    @Test
    @DisplayName("브랜드 검증 거절: 상태가 REJECTED로 변경되고 비활성화되어야 한다")
    fun reject() {
        // Arrange
        val brand = Brand.create("Toss", "toss", 1L, BrandSource.ADMIN)

        // Act
        brand.reject()

        // Assert
        assertEquals(VerificationStatus.REJECTED, brand.verificationStatus)
        assertFalse(brand.isActive)
    }

    @Test
    @DisplayName("브랜드 비활성화: isActive가 false로 변경되어야 한다")
    fun deactivate() {
        // Arrange
        val brand = Brand.create("Toss", "toss", 1L, BrandSource.ADMIN)

        // Act
        brand.deactivate()

        // Assert
        assertFalse(brand.isActive)
    }
}
