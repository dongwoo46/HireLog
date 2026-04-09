package com.hirelog.api.job.application.intake

import com.hirelog.api.job.application.intake.model.DuplicateDecision
import com.hirelog.api.job.application.intake.model.DuplicateReason
import com.hirelog.api.job.application.snapshot.port.JobSnapshotCommand
import com.hirelog.api.job.application.snapshot.port.JobSnapshotQuery
import com.hirelog.api.job.application.snapshot.view.JobSnapshotView
import com.hirelog.api.job.application.summary.command.JobSummaryGenerateCommand
import com.hirelog.api.job.application.summary.port.JobSummaryQuery
import com.hirelog.api.job.domain.type.JobSourceType
import com.hirelog.api.job.domain.type.RecruitmentPeriodType
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*

@DisplayName("JdIntakePolicy 테스트")
class JdIntakePolicyTest {

    private lateinit var policy: JdIntakePolicy
    private lateinit var snapshotCommand: JobSnapshotCommand
    private lateinit var snapshotQuery: JobSnapshotQuery
    private lateinit var summaryQuery: JobSummaryQuery

    private val validCanonicalMap = mapOf(
        "responsibilities" to List(10) {
            longText("업무내용 ${it + 1}에 대한 상세 설명입니다. ")
        },
        "requirements" to List(10) {
            longText("필수요건 ${it + 1}에 대한 상세 설명입니다. ")
        },
        "preferred" to List(5) {
            longText("우대사항 ${it + 1}에 대한 상세 설명입니다. ")
        },
        "process" to listOf(
            longText("서류 → 코딩테스트 → 기술면접 → 최종면접 ")
        )
    )

    private fun longText(base: String): String =
        base.repeat(20)   // 확실히 300자 넘게

    private val baseCommand = JobSummaryGenerateCommand(
        requestId = "req-001",
        brandName = "Toss",
        positionName = "Backend Engineer",
        source = JobSourceType.TEXT,
        sourceUrl = null,
        canonicalMap = validCanonicalMap,
        recruitmentPeriodType = RecruitmentPeriodType.UNKNOWN,
        openedDate = null,
        closedDate = null,
        skills = emptyList(),
        occurredAt = System.currentTimeMillis()
    )

    @BeforeEach
    fun setUp() {
        snapshotCommand = mockk(relaxed = true)
        snapshotQuery = mockk()
        summaryQuery = mockk()
        policy = JdIntakePolicy(snapshotCommand, snapshotQuery, summaryQuery)
    }

    // ================================
    // isValidJd
    // ================================

    @Nested
    @DisplayName("isValidJd는")
    inner class IsValidJdTest {

        @Test
        @DisplayName("유효한 JD는 true를 반환한다")
        fun shouldReturnTrueForValidJd() {
            assertThat(policy.isValidJd(baseCommand)).isTrue()
        }

        @Test
        @DisplayName("canonicalMap이 비어있으면 false를 반환한다")
        fun shouldReturnFalseWhenEmpty() {
            val cmd = baseCommand.copy(canonicalMap = emptyMap())
            assertThat(policy.isValidJd(cmd)).isFalse()
        }

        @Test
        @DisplayName("canonicalText가 300자 미만이면 false를 반환한다")
        fun shouldReturnFalseWhenTooShort() {
            val shortMap = mapOf(
                "responsibilities" to listOf("짧은 업무"),
                "requirements" to listOf("짧은 요건"),
                "preferred" to listOf("짧은 우대")
            )
            val cmd = baseCommand.copy(canonicalMap = shortMap)
            assertThat(policy.isValidJd(cmd)).isFalse()
        }

        @Test
        @DisplayName("responsibilities 섹션이 없으면 false를 반환한다")
        fun shouldReturnFalseWhenMissingResponsibilities() {
            val noResp = validCanonicalMap.toMutableMap().apply { remove("responsibilities") }
            val cmd = baseCommand.copy(canonicalMap = noResp)
            assertThat(policy.isValidJd(cmd)).isFalse()
        }

        @Test
        @DisplayName("requirements 섹션이 없으면 false를 반환한다")
        fun shouldReturnFalseWhenMissingRequirements() {
            val noReq = validCanonicalMap.toMutableMap().apply { remove("requirements") }
            val cmd = baseCommand.copy(canonicalMap = noReq)
            assertThat(policy.isValidJd(cmd)).isFalse()
        }

        @Test
        @DisplayName("preferred 섹션이 없어도 true를 반환한다 (필수/우대 혼재 JD 허용)")
        fun shouldReturnTrueWhenMissingPreferred() {
            val noPref = validCanonicalMap.toMutableMap().apply { remove("preferred") }
            val cmd = baseCommand.copy(canonicalMap = noPref)
            assertThat(policy.isValidJd(cmd)).isTrue()
        }
    }

    // ================================
    // calculateCanonicalHash
    // ================================

    @Nested
    @DisplayName("calculateCanonicalHash는")
    inner class CalculateCanonicalHashTest {

        @Test
        @DisplayName("동일한 canonicalMap에 대해 동일한 hash를 반환한다 (결정적)")
        fun shouldBeDeterministic() {
            val hash1 = policy.calculateCanonicalHash(validCanonicalMap)
            val hash2 = policy.calculateCanonicalHash(validCanonicalMap)
            assertThat(hash1).isEqualTo(hash2)
        }

        @Test
        @DisplayName("다른 내용의 canonicalMap은 다른 hash를 반환한다")
        fun shouldProduceDifferentHashForDifferentContent() {
            val other = mapOf(
                "responsibilities" to listOf("다른 업무"),
                "requirements" to listOf("다른 요건"),
                "preferred" to listOf("다른 우대")
            )
            val hash1 = policy.calculateCanonicalHash(validCanonicalMap)
            val hash2 = policy.calculateCanonicalHash(other)
            assertThat(hash1).isNotEqualTo(hash2)
        }

        @Test
        @DisplayName("key 순서가 달라도 동일한 hash를 반환한다 (정렬 보장)")
        fun shouldNormalizeKeyOrder() {
            val map1 = mapOf("responsibilities" to listOf("A"), "requirements" to listOf("B"))
            val map2 = mapOf("requirements" to listOf("B"), "responsibilities" to listOf("A"))

            assertThat(policy.calculateCanonicalHash(map1))
                .isEqualTo(policy.calculateCanonicalHash(map2))
        }

        @Test
        @DisplayName("hash 결과는 64자 16진수 문자열이다 (SHA-256)")
        fun shouldReturnSha256Format() {
            val hash = policy.calculateCanonicalHash(validCanonicalMap)
            assertThat(hash).matches("[0-9a-f]{64}")
        }
    }

    // ================================
    // buildCoreText
    // ================================

    @Nested
    @DisplayName("buildCoreText는")
    inner class BuildCoreTextTest {

        @Test
        @DisplayName("responsibilities, requirements, preferred, process 섹션을 합쳐 반환한다")
        fun shouldCombineRelevantSections() {
            val coreText = policy.buildCoreText(validCanonicalMap)

            assertThat(coreText).contains("업무내용 1")
            assertThat(coreText).contains("필수요건 1")
            assertThat(coreText).contains("우대사항 1")
            assertThat(coreText).contains("서류 → 코딩테스트 → 기술면접 → 최종면접")
        }

        @Test
        @DisplayName("해당 섹션이 없으면 빈 문자열을 반환한다")
        fun shouldReturnEmptyWhenNoRelevantSections() {
            val coreText = policy.buildCoreText(mapOf("other" to listOf("기타")))
            assertThat(coreText).isEmpty()
        }
    }

    // ================================
    // findHashDuplicate
    // ================================

    @Nested
    @DisplayName("findHashDuplicate는")
    inner class FindHashDuplicateTest {

        private val snapshotView = JobSnapshotView(
            id = 10L,
            brandId = null,
            positionId = null,
            sourceType = JobSourceType.TEXT,
            sourceUrl = null,
            canonicalSections = emptyMap(),
            recruitmentPeriodType = RecruitmentPeriodType.UNKNOWN,
            openedDate = null,
            closedDate = null
        )

        @Test
        @DisplayName("Snapshot이 없으면 null을 반환한다")
        fun shouldReturnNullWhenNoSnapshot() {
            every { snapshotQuery.getSnapshotByCanonicalHash("hash-abc") } returns null

            val result = policy.findHashDuplicate("hash-abc")

            assertThat(result).isNull()
        }

        @Test
        @DisplayName("Snapshot 존재 + JobSummary 없으면 Reprocessable을 반환한다")
        fun shouldReturnReprocessableWhenNoSummary() {
            every { snapshotQuery.getSnapshotByCanonicalHash("hash-abc") } returns snapshotView
            every { summaryQuery.findIdByJobSnapshotId(10L) } returns null

            val result = policy.findHashDuplicate("hash-abc")

            assertThat(result).isInstanceOf(DuplicateDecision.Reprocessable::class.java)
            assertThat((result as DuplicateDecision.Reprocessable).existingSnapshotId).isEqualTo(10L)
        }

        @Test
        @DisplayName("Snapshot 존재 + JobSummary 존재하면 Duplicate를 반환한다")
        fun shouldReturnDuplicateWhenSummaryExists() {
            every { snapshotQuery.getSnapshotByCanonicalHash("hash-abc") } returns snapshotView
            every { summaryQuery.findIdByJobSnapshotId(10L) } returns 99L

            val result = policy.findHashDuplicate("hash-abc")

            assertThat(result).isInstanceOf(DuplicateDecision.Duplicate::class.java)
            val duplicate = result as DuplicateDecision.Duplicate
            assertThat(duplicate.reason).isEqualTo(DuplicateReason.HASH)
            assertThat(duplicate.existingSnapshotId).isEqualTo(10L)
            assertThat(duplicate.existingSummaryId).isEqualTo(99L)
        }
    }

    // ================================
    // decideDuplicate
    // ================================

    @Nested
    @DisplayName("decideDuplicate는")
    inner class DecideDuplicateTest {

        @Test
        @DisplayName("canonicalHash 중복이면 Fast-path로 Duplicate를 반환한다")
        fun shouldReturnDuplicateOnHashMatch() {
            val hash = policy.calculateCanonicalHash(validCanonicalMap)
            val snapshotView = JobSnapshotView(
                id = 5L, brandId = null, positionId = null,
                sourceType = JobSourceType.TEXT, sourceUrl = null,
                canonicalSections = emptyMap(),
                recruitmentPeriodType = RecruitmentPeriodType.UNKNOWN,
                openedDate = null, closedDate = null
            )

            every { snapshotQuery.getSnapshotByCanonicalHash(hash) } returns snapshotView
            every { summaryQuery.findIdByJobSnapshotId(5L) } returns 77L

            val result = policy.decideDuplicate(baseCommand, policy.generateIntakeHashes(validCanonicalMap))

            assertThat(result).isInstanceOf(DuplicateDecision.Duplicate::class.java)
            assertThat((result as DuplicateDecision.Duplicate).reason).isEqualTo(DuplicateReason.HASH)
        }

        @Test
        @DisplayName("중복 없으면 NotDuplicate를 반환한다")
        fun shouldReturnNotDuplicateWhenNoDuplicate() {
            val hashes = policy.generateIntakeHashes(validCanonicalMap)

            every { snapshotQuery.getSnapshotByCanonicalHash(hashes.canonicalHash) } returns null
            every { snapshotCommand.findAllBySourceUrl(any()) } returns emptyList()
            every { snapshotCommand.findAllByDateRange(any(), any()) } returns emptyList()
            every { snapshotCommand.findSimilarByCoreText(any(), any()) } returns emptyList()

            val result = policy.decideDuplicate(baseCommand, hashes)

            assertThat(result).isEqualTo(DuplicateDecision.NotDuplicate)
        }
    }
}
