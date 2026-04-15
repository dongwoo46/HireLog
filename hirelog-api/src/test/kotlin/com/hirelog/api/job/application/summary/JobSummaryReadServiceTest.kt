package com.hirelog.api.job.application.summary

import com.hirelog.api.job.application.summary.port.JobSummaryQuery
import com.hirelog.api.job.application.summary.query.JobSummarySearchItem
import com.hirelog.api.job.application.summary.query.JobSummarySearchQuery
import com.hirelog.api.job.application.summary.query.JobSummarySearchResult
import com.hirelog.api.job.infra.persistence.opensearch.JobSummaryOpenSearchQuery
import com.hirelog.api.relation.application.memberjobsummary.port.MemberJobSummaryQuery
import com.hirelog.api.relation.application.memberjobsummary.view.JobSummarySavedStateView
import com.hirelog.api.relation.domain.type.MemberJobSummarySaveType
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import java.time.LocalDateTime

@DisplayName("JobSummaryReadService 테스트")
class JobSummaryReadServiceTest {

    private lateinit var service: JobSummaryReadService
    private lateinit var jobSummaryQuery: JobSummaryQuery
    private lateinit var openSearchQuery: JobSummaryOpenSearchQuery
    private lateinit var memberJobSummaryQuery: MemberJobSummaryQuery

    @BeforeEach
    fun setUp() {
        jobSummaryQuery = mockk()
        openSearchQuery = mockk()
        memberJobSummaryQuery = mockk()
        service = JobSummaryReadService(jobSummaryQuery, openSearchQuery, memberJobSummaryQuery)
    }

    private fun searchItem(id: Long) = JobSummarySearchItem(
        id = id,
        brandName = "Toss",
        brandPositionName = "Backend Engineer",
        positionCategoryName = "Engineering",
        careerType = "EXPERIENCED",
        companyDomain = null,
        companySize = null,
        summaryText = "요약",
        techStackParsed = listOf("Kotlin"),
        createdAt = LocalDateTime.now()
    )

    private fun searchResultOf(vararg items: JobSummarySearchItem) = JobSummarySearchResult(
        items = items.toList(),
        size = items.size,
        hasNext = false,
        nextCursor = null
    )

    private fun savedStateOf(jobSummaryId: Long, memberJobSummaryId: Long, saveType: MemberJobSummarySaveType) =
        JobSummarySavedStateView(
            jobSummaryId = jobSummaryId,
            memberJobSummaryId = memberJobSummaryId,
            saveType = saveType
        )

    @Nested
    @DisplayName("search 메서드는")
    inner class SearchTest {

        @Test
        @DisplayName("검색 결과가 비어있으면 memberJobSummaryQuery를 호출하지 않고 즉시 반환한다")
        fun shouldSkipEnrichmentWhenEmpty() {
            val query = JobSummarySearchQuery()
            every { openSearchQuery.search(query) } returns JobSummarySearchResult(
                items = emptyList(), size = 0, hasNext = false, nextCursor = null
            )

            val result = service.search(query, memberId = 1L)

            assertThat(result.items).isEmpty()
            verify(exactly = 0) { memberJobSummaryQuery.findSavedStatesByJobSummaryIds(any(), any()) }
        }

        @Test
        @DisplayName("저장된 항목에는 isSaved=true와 memberJobSummaryId, memberSaveType이 채워진다")
        fun shouldEnrichSavedItems() {
            val query = JobSummarySearchQuery()
            every { openSearchQuery.search(query) } returns searchResultOf(searchItem(10L))
            every {
                memberJobSummaryQuery.findSavedStatesByJobSummaryIds(1L, setOf(10L))
            } returns mapOf(10L to savedStateOf(10L, 999L, MemberJobSummarySaveType.SAVED))

            val result = service.search(query, memberId = 1L)

            with(result.items[0]) {
                assertThat(isSaved).isTrue()
                assertThat(memberJobSummaryId).isEqualTo(999L)
                assertThat(memberSaveType).isEqualTo("SAVED")
            }
        }

        @Test
        @DisplayName("저장되지 않은 항목에는 isSaved=false이고 memberJobSummaryId는 null이다")
        fun shouldMarkUnsavedItemsCorrectly() {
            val query = JobSummarySearchQuery()
            every { openSearchQuery.search(query) } returns searchResultOf(searchItem(20L))
            every {
                memberJobSummaryQuery.findSavedStatesByJobSummaryIds(1L, setOf(20L))
            } returns emptyMap()

            val result = service.search(query, memberId = 1L)

            with(result.items[0]) {
                assertThat(isSaved).isFalse()
                assertThat(memberJobSummaryId).isNull()
                assertThat(memberSaveType).isNull()
            }
        }

        @Test
        @DisplayName("여러 항목 중 일부만 저장된 경우 각 항목의 저장 상태가 독립적으로 반영된다")
        fun shouldEnrichMixedItems() {
            val query = JobSummarySearchQuery()
            every { openSearchQuery.search(query) } returns searchResultOf(searchItem(1L), searchItem(2L))
            every {
                memberJobSummaryQuery.findSavedStatesByJobSummaryIds(1L, setOf(1L, 2L))
            } returns mapOf(1L to savedStateOf(1L, 100L, MemberJobSummarySaveType.APPLY))

            val result = service.search(query, memberId = 1L)

            assertThat(result.items[0].isSaved).isTrue()
            assertThat(result.items[0].memberSaveType).isEqualTo("APPLY")
            assertThat(result.items[1].isSaved).isFalse()
            assertThat(result.items[1].memberJobSummaryId).isNull()
        }

        @Test
        @DisplayName("hasNext와 nextCursor는 OpenSearch 결과를 그대로 전달한다")
        fun shouldPreservePaginationMetadata() {
            val query = JobSummarySearchQuery()
            every { openSearchQuery.search(query) } returns JobSummarySearchResult(
                items = listOf(searchItem(1L)),
                size = 1,
                hasNext = true,
                nextCursor = "encodedCursor"
            )
            every { memberJobSummaryQuery.findSavedStatesByJobSummaryIds(any(), any()) } returns emptyMap()

            val result = service.search(query, memberId = 1L)

            assertThat(result.hasNext).isTrue()
            assertThat(result.nextCursor).isEqualTo("encodedCursor")
        }

        @Test
        @DisplayName("memberId별로 올바른 jobSummaryIds를 전달하여 저장 상태를 조회한다")
        fun shouldPassCorrectMemberIdAndJobSummaryIds() {
            val query = JobSummarySearchQuery()
            every { openSearchQuery.search(query) } returns searchResultOf(
                searchItem(10L), searchItem(20L), searchItem(30L)
            )
            every {
                memberJobSummaryQuery.findSavedStatesByJobSummaryIds(7L, setOf(10L, 20L, 30L))
            } returns emptyMap()

            service.search(query, memberId = 7L)

            verify { memberJobSummaryQuery.findSavedStatesByJobSummaryIds(7L, setOf(10L, 20L, 30L)) }
        }
    }

    @Nested
    @DisplayName("getDetail 메서드는")
    inner class GetDetailTest {

        @Test
        @DisplayName("jobSummaryQuery.findDetailById에 위임하고 그 결과를 반환한다")
        fun shouldDelegateToQueryAndReturnResult() {
            every { jobSummaryQuery.findDetailById(42L, 1L) } returns null

            val result = service.getDetail(42L, 1L)

            assertThat(result).isNull()
            verify { jobSummaryQuery.findDetailById(42L, 1L) }
        }
    }
}
