package com.hirelog.api.job.domain.model

import com.hirelog.api.job.application.rag.model.RagIntent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("RagQueryLog 도메인 테스트")
class RagQueryLogTest {

    @Nested
    @DisplayName("create 팩토리는")
    inner class CreateTest {

        @Test
        @DisplayName("필수 필드를 정상적으로 세팅한다")
        fun shouldSetRequiredFields() {
            val log = RagQueryLog.create(
                memberId = 1L,
                question = "백엔드 공고 추천해줘",
                intent = RagIntent.DOCUMENT_SEARCH,
                parsedText = "백엔드 Spring Boot",
                parsedFiltersJson = null,
                contextJson = null,
                answer = "관련 공고 3건을 찾았습니다.",
                reasoning = null,
                evidencesJson = null,
                sourcesJson = null
            )

            assertThat(log.memberId).isEqualTo(1L)
            assertThat(log.question).isEqualTo("백엔드 공고 추천해줘")
            assertThat(log.intent).isEqualTo(RagIntent.DOCUMENT_SEARCH)
            assertThat(log.parsedText).isEqualTo("백엔드 Spring Boot")
            assertThat(log.answer).isEqualTo("관련 공고 3건을 찾았습니다.")
        }

        @Test
        @DisplayName("nullable 필드는 null로 세팅된다")
        fun shouldAllowNullableFields() {
            val log = RagQueryLog.create(
                memberId = 2L,
                question = "지원 통계 알려줘",
                intent = RagIntent.STATISTICS,
                parsedText = null,
                parsedFiltersJson = null,
                contextJson = null,
                answer = "저장한 공고 없음",
                reasoning = null,
                evidencesJson = null,
                sourcesJson = null
            )

            assertThat(log.parsedText).isNull()
            assertThat(log.parsedFiltersJson).isNull()
            assertThat(log.contextJson).isNull()
            assertThat(log.reasoning).isNull()
            assertThat(log.evidencesJson).isNull()
            assertThat(log.sourcesJson).isNull()
        }

        @Test
        @DisplayName("JSON 필드를 포함한 전체 필드를 정상 세팅한다")
        fun shouldSetAllFields() {
            val log = RagQueryLog.create(
                memberId = 3L,
                question = "핀테크 패턴 분석",
                intent = RagIntent.STATISTICS,
                parsedText = "핀테크 Kafka",
                parsedFiltersJson = """{"careerType":"NEW"}""",
                contextJson = """{"aggregations":[]}""",
                answer = "Kafka 스킬이 67% 등장합니다.",
                reasoning = "저장 공고 12건 중 8건에서 Kafka 확인",
                evidencesJson = """[{"type":"AGGREGATION","summary":"Kafka 8건"}]""",
                sourcesJson = null
            )

            assertThat(log.intent).isEqualTo(RagIntent.STATISTICS)
            assertThat(log.parsedFiltersJson).isEqualTo("""{"careerType":"NEW"}""")
            assertThat(log.contextJson).isEqualTo("""{"aggregations":[]}""")
            assertThat(log.reasoning).isEqualTo("저장 공고 12건 중 8건에서 Kafka 확인")
            assertThat(log.evidencesJson).isEqualTo("""[{"type":"AGGREGATION","summary":"Kafka 8건"}]""")
            assertThat(log.sourcesJson).isNull()
        }
    }
}