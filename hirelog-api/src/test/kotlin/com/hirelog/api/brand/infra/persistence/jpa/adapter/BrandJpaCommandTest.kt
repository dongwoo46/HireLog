package com.hirelog.api.brand.infra.persistence.jpa.adapter

import com.hirelog.api.brand.domain.Brand
import com.hirelog.api.brand.infra.persistence.jpa.repository.BrandJpaRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import java.util.Optional

@DisplayName("BrandJpaCommand Adapter 테스트")
class BrandJpaCommandTest {

    private val repository: BrandJpaRepository = mockk()
    private val adapter = BrandJpaCommand(repository)

    @Test
    @DisplayName("save: Repository에 위임해야 한다")
    fun save() {
        // given
        val brand = mockk<Brand>()
        every { repository.save(brand) } returns brand

        // when
        val result = adapter.save(brand)

        // then
        assertEquals(brand, result)
        verify(exactly = 1) { repository.save(brand) }
    }

    @Test
    @DisplayName("findByNormalizedName: Repository에 위임해야 한다")
    fun findByNormalizedName() {
        // given
        val brand = mockk<Brand>()
        every { repository.findByNormalizedName("name") } returns brand

        // when
        val result = adapter.findByNormalizedName("name")

        // then
        assertEquals(brand, result)
        verify(exactly = 1) { repository.findByNormalizedName("name") }
    }

    @Test
    @DisplayName("findById: Repository findByIdOrNull에 위임해야 한다")
    fun findById() {
        // given
        val brand = mockk<Brand>()
        every { repository.findById(1L) } returns Optional.of(brand)

        // when
        val result = adapter.findById(1L)

        // then
        assertEquals(brand, result)
        verify(exactly = 1) { repository.findById(1L) }
    }
}
