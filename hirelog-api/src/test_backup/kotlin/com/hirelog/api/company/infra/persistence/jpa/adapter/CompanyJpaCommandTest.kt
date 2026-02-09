package com.hirelog.api.company.infra.persistence.jpa.adapter

import com.hirelog.api.company.domain.Company
import com.hirelog.api.company.infra.persistence.jpa.repository.CompanyJpaRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.Optional

@DisplayName("CompanyJpaCommand Adapter 테스트")
class CompanyJpaCommandTest {

    private val repository: CompanyJpaRepository = mockk()
    private val adapter = CompanyJpaCommand(repository)

    @Test
    @DisplayName("save: Repository에 위임해야 한다")
    fun save() {
        val company = mockk<Company>()
        every { repository.save(company) } returns company

        val result = adapter.save(company)

        assertEquals(company, result)
        verify(exactly = 1) { repository.save(company) }
    }

    @Test
    @DisplayName("findById: Repository에 위임해야 한다")
    fun findById() {
        val company = mockk<Company>()
        every { repository.findById(1L) } returns Optional.of(company)

        val result = adapter.findById(1L)

        assertEquals(company, result)
        verify(exactly = 1) { repository.findById(1L) }
    }

    @Test
    @DisplayName("findByNormalizedName: Repository에 위임해야 한다")
    fun findByName() {
        val company = mockk<Company>()
        every { repository.findByNormalizedName("toss") } returns company

        val result = adapter.findByNormalizedName("toss")

        assertEquals(company, result)
        verify(exactly = 1) { repository.findByNormalizedName("toss") }
    }
}
