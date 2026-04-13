# Vladimir Khorikov 단위 테스트 원칙 (이 프로젝트 적용 기준)

> 참고: *Unit Testing: Principles, Practices, and Patterns* — Vladimir Khorikov

---

## 1. 좋은 단위 테스트의 4가지 속성

| 속성 | 의미 |
|---|---|
| **회귀 보호** | 코드 변경 시 버그를 잡아낸다 |
| **리팩터링 내성** | 구현이 바뀌어도 테스트가 거짓 양성(false positive)을 내지 않는다 |
| **빠른 피드백** | 실행이 빠르다 (단위 테스트는 ms 단위) |
| **유지 보수 가능성** | 읽기 쉽고 수정이 쉽다 |

**가장 흔한 실수**: 리팩터링 내성 없는 테스트 작성.
→ 구현 세부사항(private 메서드, 내부 상태)을 직접 검증하면 코드를 리팩터링할 때마다 테스트가 깨진다.

---

## 2. 고전파 vs 런던파 — 이 프로젝트의 선택

| 구분 | 고전파 (Detroit) | 런던파 (London/Mockist) |
|---|---|---|
| 협력 객체 | 가능하면 실제 객체 사용 | 테스트 대상 제외 전부 mock |
| mock 사용 범위 | **아웃-오브-프로세스 의존성만** mock | 모든 의존성 mock |
| 단점 | 테스트 간 결합 가능성 | 구현 세부사항과 결합 → 리팩터링 내성 약화 |

**이 프로젝트 선택: 고전파**

아웃-오브-프로세스 의존성 = mock 대상:
- `JobSummaryEmbedding` → HTTP (임베딩 서버)
- `JobSummaryOpenSearchAdapter` → OpenSearch
- `JobSummaryIndexManager` → OpenSearch
- `JobSummaryCommand` / `JobSummaryQuery` → PostgreSQL
- Kafka Consumer → Kafka

인-프로세스 의존성 = 실제 객체 사용 권장:
- 도메인 모델 (`JobSummary`, `JobSummaryInsight`, ...)
- Payload 변환 (`JobSummaryOutboxPayload.from()`, `JobSummarySearchPayload.from()`)
- Jackson ObjectMapper

---

## 3. Stub vs Mock — 핵심 규칙

| 구분 | 역할 | 검증 여부 |
|---|---|---|
| **Stub** | 쿼리(데이터 반환), 들어오는 상호작용 | **검증하지 않음** |
| **Mock** | 커맨드(상태 변경), 나가는 상호작용 | **반드시 검증** |

```kotlin
// ✅ 올바른 사용 — Stub은 검증 안 함
every { summaryCommand.findAllForReindex(0L, 50) } returns listOf(summary)
// verify { summaryCommand.findAllForReindex(any(), any()) }  ← 금지

// ✅ 올바른 사용 — Mock은 검증함
every { openSearchAdapter.index(capture(slot)) } just Runs
// ...
assertThat(slot.captured.embeddingVector).isEqualTo(expectedVector)  // 커맨드 결과 검증
```

이 프로젝트 기준:

| 의존성 | 메서드 | 종류 | 검증 |
|---|---|---|---|
| `summaryCommand` | `findAllForReindex()` | Stub (쿼리) | 검증 안 함 |
| `openSearchAdapter` | `findMissingEmbedding()` | Stub (쿼리) | 검증 안 함 |
| `openSearchAdapter` | `index()` | Mock (커맨드) | **검증** |
| `openSearchAdapter` | `updateEmbeddingVector()` | Mock (커맨드) | **검증** |
| `indexManager` | `deleteIndex()` | Mock (커맨드) | **검증** |
| `indexManager` | `createIndexIfNotExists()` | Mock (커맨드) | **검증** |
| `embeddingPort` | `embed()` | Stub (쿼리, HTTP) | 검증 안 함 |

---

## 4. AAA 패턴

```kotlin
@Test
@DisplayName("임베딩 성공 시 벡터와 함께 인덱싱하고 성공 건수를 반환한다")
fun shouldIndexWithVectorAndReturnSuccessCount() {
    // Arrange
    val expectedVector = listOf(0.1f, 0.2f, 0.3f)
    val summary = createSummaryMock(id = 1L)
    every { summaryCommand.findAllForReindex(0L, 10) } returns listOf(summary)
    every { embeddingPort.embed(any()) } returns expectedVector
    val slot = slot<JobSummarySearchPayload>()
    every { openSearchAdapter.index(capture(slot)) } just Runs

    // Act
    val result = service.reindexAll(batchSize = 10)

    // Assert
    assertThat(result).isEqualTo(1)
    assertThat(slot.captured.embeddingVector).isEqualTo(expectedVector)
}
```

규칙:
- Act는 **한 줄** (테스트당 하나의 동작)
- Arrange가 너무 길면 → 헬퍼 함수 또는 `@BeforeEach` 분리
- Assert는 **관측 가능한 결과**만 (반환값 + 아웃-오브-프로세스 상호작용)

---

## 5. 테스트 이름

**나쁜 예** (구현 중심):
```
findAllForReindex가_lastId보다_큰_id를_가진_엔티티를_반환한다
```

**좋은 예** (행동/시나리오 중심):
```
임베딩_실패한_문서는_건너뛰고_나머지_성공_건수를_반환한다
DB가_비어있으면_인덱스를_재생성하고_0을_반환한다
임베딩_서버_장애_시_null_벡터로_인덱싱하고_Kafka_메시지를_커밋한다
```

형식: `[시나리오]_[기대_결과]`
→ 테스트 이름만 읽어도 무엇을 테스트하는지 알아야 한다

---

## 6. 도메인 객체 생성 전략

`JobSummary`는 `protected constructor` → 직접 생성 불가.

```kotlin
// ✅ 방법 1: 팩토리 메서드 (id 제어 불필요 시)
val summary = JobSummary.create(
    jobSnapshotId = 1L, brandId = 100L, brandName = "토스", ...
)

// ✅ 방법 2: mockk relaxed (id 제어 필요 시 — 커서 테스트 등)
fun createSummaryMock(id: Long): JobSummary = mockk(relaxed = true) {
    every { this@mockk.id } returns id
    every { careerType } returns CareerType.EXPERIENCED
    every { createdAt } returns LocalDateTime.of(2026, 4, 13, 12, 0, 0)
    every { insight } returns JobSummaryInsight.empty()
}
```

`mockk(relaxed = true)` 주의:
- `enum` 타입 필드는 반드시 명시 (`careerType`, ...)
- `LocalDateTime` 필드도 명시 (`.format()` 호출이 있으므로)
- `insight`는 `JobSummaryInsight.empty()`로 실제 객체 제공

---

## 7. 이 프로젝트 테스트 파일 위치

```
src/test/kotlin/com/hirelog/api/
├── job/
│   ├── application/
│   │   └── summary/
│   │       └── JobSummaryAdminServiceTest.kt   ← reindexAll, reindexMissingEmbeddings
│   └── infra/
│       └── kafka/
│           └── consumer/
│               └── JobSummaryIndexingConsumerTest.kt  ← 임베딩 그레이스풀 처리
```

---

## 8. 자주 범하는 실수 (이 프로젝트 적용)

```kotlin
// ❌ 잘못된 예 — Stub 검증 (리팩터링 내성 약화)
verify(exactly = 1) { summaryCommand.findAllForReindex(0L, 50) }

// ❌ 잘못된 예 — 구현 세부사항 테스트 (private 로직 직접 검증)
// handleIndex()는 private → consume()을 통해 간접 테스트

// ❌ 잘못된 예 — 하나의 테스트에 여러 Act
service.reindexAll(50)
service.reindexMissingEmbeddings(100)  // 두 번째 Act → 별도 테스트로 분리

// ✅ 올바른 예 — 관측 가능한 결과만 검증
val result = service.reindexAll(batchSize = 10)
assertThat(result).isEqualTo(2)
verify(exactly = 2) { openSearchAdapter.index(any()) }
```