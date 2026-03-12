package com.hirelog.api.job.domain.model

import com.hirelog.api.common.domain.LlmProvider
import com.hirelog.api.job.domain.type.CareerType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*

@DisplayName("JobSummary 도메인 테스트")
class JobSummaryTest {

    private fun makeInsight() = JobSummaryInsight.empty()

    private fun makeSummary(isActive: Boolean = true): JobSummary = JobSummary.create(
        jobSnapshotId = 1L,
        brandId = 10L,
        brandName = "Toss",
        companyId = null,
        companyName = null,
        positionId = 20L,
        positionName = "backend_engineer",
        brandPositionId = 30L,
        brandPositionName = "Backend Engineer",
        positionCategoryId = 5L,
        positionCategoryName = "Engineering",
        careerType = CareerType.EXPERIENCED,
        careerYears = "3년 이상",
        summaryText = "요약입니다",
        responsibilities = "업무입니다",
        requiredQualifications = "자격요건입니다",
        preferredQualifications = null,
        techStack = "Kotlin, Spring",
        recruitmentProcess = null,
        insight = makeInsight(),
        llmProvider = LlmProvider.GEMINI,
        llmModel = "gemini-2.0-flash",
        sourceUrl = null
    )

    @Nested
    @DisplayName("create 팩토리는")
    inner class CreateTest {

        @Test
        @DisplayName("isActive=true로 생성된다")
        fun shouldCreateWithActiveTrue() {
            val summary = makeSummary()
            assertThat(summary.isActive).isTrue()
            assertThat(summary.brandName).isEqualTo("Toss")
            assertThat(summary.careerType).isEqualTo(CareerType.EXPERIENCED)
        }
    }

    @Nested
    @DisplayName("deactivate는")
    inner class DeactivateTest {

        @Test
        @DisplayName("isActive=true이면 false로 변경한다")
        fun shouldSetIsActiveFalse() {
            val summary = makeSummary()
            summary.deactivate()
            assertThat(summary.isActive).isFalse()
        }

        @Test
        @DisplayName("이미 비활성화된 경우 무시한다 (멱등)")
        fun shouldBeIdempotent() {
            val summary = makeSummary()
            summary.deactivate()
            summary.deactivate() // 중복 호출

            assertThat(summary.isActive).isFalse()
        }
    }

    @Nested
    @DisplayName("activate는")
    inner class ActivateTest {

        @Test
        @DisplayName("isActive=false이면 true로 변경한다")
        fun shouldSetIsActiveTrue() {
            val summary = makeSummary()
            summary.deactivate()
            summary.activate()
            assertThat(summary.isActive).isTrue()
        }

        @Test
        @DisplayName("이미 활성화된 경우 무시한다 (멱등)")
        fun shouldBeIdempotent() {
            val summary = makeSummary()
            summary.activate() // 이미 active
            assertThat(summary.isActive).isTrue()
        }
    }

    @Nested
    @DisplayName("applyCompany는")
    inner class ApplyCompanyTest {

        @Test
        @DisplayName("companyId가 null인 경우 companyId와 companyName을 설정한다")
        fun shouldApplyCompanyWhenNull() {
            val summary = makeSummary()
            summary.applyCompany(companyId = 100L, companyName = "비바리퍼블리카")

            assertThat(summary.companyId).isEqualTo(100L)
            assertThat(summary.companyName).isEqualTo("비바리퍼블리카")
        }

        @Test
        @DisplayName("이미 companyId가 설정된 경우 두 번째 호출은 무시된다")
        fun shouldIgnoreWhenCompanyAlreadyApplied() {
            val summary = makeSummary()
            summary.applyCompany(companyId = 100L, companyName = "비바리퍼블리카")
            summary.applyCompany(companyId = 200L, companyName = "다른회사") // 무시되어야 함

            assertThat(summary.companyId).isEqualTo(100L)
            assertThat(summary.companyName).isEqualTo("비바리퍼블리카")
        }
    }
}
