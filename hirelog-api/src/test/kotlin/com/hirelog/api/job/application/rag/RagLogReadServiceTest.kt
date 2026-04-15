package com.hirelog.api.job.application.rag

import com.hirelog.api.common.application.port.PagedResult
import com.hirelog.api.job.application.rag.model.RagIntent
import com.hirelog.api.job.application.rag.port.RagQueryLogQuery
import com.hirelog.api.job.application.rag.view.RagQueryLogView
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("RagLogReadService 테스트")
class RagLogReadServiceTest {

    private lateinit var ragQueryLogQuery: RagQueryLogQuery
    private lateinit var service: RagLogReadService

    @BeforeEach
    fun setUp() {
        ragQueryLogQuery = mockk()
        service = RagLogReadService(ragQueryLogQuery)
    }

    private fun emptyPage(page: Int = 0, size: Int = 20) =
        PagedResult.empty<RagQueryLogView>(page, size)

    private fun stubView(id: Long = 1L, memberId: Long = 10L) = RagQueryLogView(
        id = id,
        memberId = memberId,
        question = "질문",
        intent = RagIntent.DOCUMENT_SEARCH,
        parsedText = null,
        parsedFiltersJson = null,
        contextJson = null,
        answer = "답변",
        reasoning = null,
        evidencesJson = null,
        sourcesJson = null,
        createdAt = LocalDateTime.now()
    )

    @Nested
    @DisplayName("searchAdmin은")
    inner class SearchAdminTest {

        @Test
        @DisplayName("memberId=null로 전달하면 전체 로그를 조회한다")
        fun shouldSearchAllWhenMemberIdIsNull() {
            every { ragQueryLogQuery.search(null, null, null, null, 0, 20) } returns emptyPage()

            service.searchAdmin(
                memberId = null,
                intent = null,
                dateFrom = null,
                dateTo = null,
                page = 0,
                size = 20
            )

            verify(exactly = 1) { ragQueryLogQuery.search(null, null, null, null, 0, 20) }
        }

        @Test
        @DisplayName("memberId를 지정하면 해당 멤버 로그만 조회한다")
        fun shouldFilterByMemberIdWhenProvided() {
            val memberId = 99L
            every { ragQueryLogQuery.search(memberId, null, null, null, 0, 20) } returns emptyPage()

            service.searchAdmin(memberId = memberId, intent = null, dateFrom = null, dateTo = null, page = 0, size = 20)

            verify(exactly = 1) { ragQueryLogQuery.search(memberId, null, null, null, 0, 20) }
        }

        @Test
        @DisplayName("intent, dateFrom, dateTo 필터를 그대로 전달한다")
        fun shouldPassAllFiltersThrough() {
            val from = LocalDate.of(2026, 1, 1)
            val to = LocalDate.of(2026, 1, 31)
            every {
                ragQueryLogQuery.search(null, RagIntent.STATISTICS, from, to, 1, 10)
            } returns emptyPage(1, 10)

            service.searchAdmin(
                memberId = null,
                intent = RagIntent.STATISTICS,
                dateFrom = from,
                dateTo = to,
                page = 1,
                size = 10
            )

            verify(exactly = 1) { ragQueryLogQuery.search(null, RagIntent.STATISTICS, from, to, 1, 10) }
        }

        @Test
        @DisplayName("PagedResult를 그대로 반환한다")
        fun shouldReturnPagedResult() {
            val view = stubView()
            val expected = PagedResult.of(listOf(view), 0, 20, 1L)
            every { ragQueryLogQuery.search(null, null, null, null, 0, 20) } returns expected

            val result = service.searchAdmin(null, null, null, null, 0, 20)

            assertThat(result.totalElements).isEqualTo(1L)
            assertThat(result.items).hasSize(1)
            assertThat(result.items[0].id).isEqualTo(view.id)
        }
    }

    @Nested
    @DisplayName("searchMine은")
    inner class SearchMineTest {

        @Test
        @DisplayName("항상 요청자 memberId를 첫 번째 인자로 전달한다")
        fun shouldAlwaysPassCallerMemberId() {
            val memberId = 42L
            every { ragQueryLogQuery.search(memberId, null, null, null, 0, 20) } returns emptyPage()

            service.searchMine(memberId = memberId, intent = null, dateFrom = null, dateTo = null, page = 0, size = 20)

            verify(exactly = 1) { ragQueryLogQuery.search(memberId, null, null, null, 0, 20) }
        }

        @Test
        @DisplayName("intent, dateFrom, dateTo 필터를 그대로 전달한다")
        fun shouldPassFiltersWithMemberId() {
            val memberId = 5L
            val from = LocalDate.of(2026, 4, 1)
            every {
                ragQueryLogQuery.search(memberId, RagIntent.EXPERIENCE_ANALYSIS, from, null, 0, 10)
            } returns emptyPage(0, 10)

            service.searchMine(
                memberId = memberId,
                intent = RagIntent.EXPERIENCE_ANALYSIS,
                dateFrom = from,
                dateTo = null,
                page = 0,
                size = 10
            )

            verify(exactly = 1) {
                ragQueryLogQuery.search(memberId, RagIntent.EXPERIENCE_ANALYSIS, from, null, 0, 10)
            }
        }
    }

    @Nested
    @DisplayName("findById는")
    inner class FindByIdTest {

        @Test
        @DisplayName("존재하는 id이면 RagQueryLogView를 반환한다")
        fun shouldReturnViewWhenExists() {
            val view = stubView(id = 7L)
            every { ragQueryLogQuery.findById(7L) } returns view

            val result = service.findById(7L)

            assertThat(result).isNotNull
            assertThat(result!!.id).isEqualTo(7L)
        }

        @Test
        @DisplayName("존재하지 않는 id이면 null을 반환한다")
        fun shouldReturnNullWhenNotFound() {
            every { ragQueryLogQuery.findById(999L) } returns null

            val result = service.findById(999L)

            assertThat(result).isNull()
        }
    }
}
