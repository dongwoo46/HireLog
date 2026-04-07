# HireLog 부하 테스트

## 시나리오 A: OpenSearch 검색 부하 테스트

### 왜 테스트하는가

검색 API(`GET /api/job-summary/search`)는 HireLog에서 유일하게 **동기 + 외부 저장소(OpenSearch) 조회**가 발생하는 API다.
비동기 파이프라인과 달리 요청이 들어오는 즉시 OpenSearch를 조회하고 응답을 반환하기 때문에, 동시 요청이 몰리면 latency가 직접 증가한다.

### 핵심 포인트 (코드 근거)

`JobSummaryReadService.search()`는 로그인 여부에 따라 쿼리 수가 달라진다.

```
비로그인: OpenSearch 쿼리 1회
로그인:   OpenSearch 쿼리 1회
        + DB 쿼리 1회 (MemberJobSummary saved states enrichment)
```

로그인 사용자는 매 검색마다 **DB 추가 조회**가 발생한다.
이 비용이 트래픽이 몰릴 때 얼마나 커지는지, 비로그인 대비 얼마나 느린지 수치로 확인해야 한다.

### 무엇을 알아내야 하는가

1. **VU 증가에 따른 latency 변화**: p50/p95/p99가 어느 VU 구간에서 급등하는가
2. **비로그인 vs 로그인 latency 차이**: DB enrichment 추가 비용이 실제로 얼마인가 (ms 단위로)
3. **OpenSearch 한계**: 어느 시점에서 OpenSearch가 병목이 되는가 (CPU, heap 급등)
4. **개선 효과**: Redis 캐시 적용 후 p99가 얼마나 줄어드는가

### 포폴 스토리 구조

```
[발견] VU 200 기준 로그인 검색 p99 = Xms, 비로그인 대비 Yms 더 느림
[원인] 매 검색마다 MemberJobSummary DB 조회 발생
[개선] 검색 결과 Redis 캐싱 or saved states IN-query 최적화
[결과] p99 X'ms로 감소, 캐시 히트율 Z%
```

---

## 시나리오 B: JD 파이프라인 처리량 테스트

### 왜 테스트하는가

JD 요약 요청은 **비동기 파이프라인** 구조다.
Intake API(`POST /api/job-summary/text`)는 즉시 200을 반환하지만,
실제 처리는 `Kafka → preprocess-pipeline → LLM → OpenSearch` 순서로 이어진다.

문제는 **Intake는 빠르고, 파이프라인은 느리다**는 것이다.
초당 N건을 계속 밀어 넣으면 파이프라인이 감당하지 못하고 적체가 쌓인다.
어느 속도에서 시스템이 무너지는지, 어디서 병목이 생기는지 찾아야 한다.

### 핵심 포인트 (코드 근거)

파이프라인에는 두 개의 처리량 제한 지점이 존재한다.

**1. Kafka consumer concurrency = 3**
```kotlin
// KafkaConsumerConfig.kt
setConcurrency(3)
```
응답 메시지를 동시에 3개만 처리한다. 적체가 쌓이면 Kafka consumer lag으로 나타난다.

**2. hirelogTaskExecutor: core=8, max=16, queue=200**
```kotlin
// ExecutorConfig.kt
executor.corePoolSize = 8
executor.maxPoolSize = 16
executor.queueCapacity = 200
```
LLM 호출 후 후처리가 이 executor에서 실행된다.
queue 200개가 꽉 차면 `RejectedExecutionException`이 발생한다.

**3. Kafka 재시도 정책: 3회 / 1초 간격 → DLT**
```kotlin
// KafkaConsumerConfig.kt
FixedBackOff(1000L, 3L)
```
처리 실패 시 3회 재시도 후 DLT로 빠진다. DLT에 메시지가 쌓이면 처리 실패를 의미한다.

### 무엇을 알아내야 하는가

**k6로 확인하는 것:**
- Intake API p95가 1초를 넘는가: 넘으면 DB(HikariCP) 문제
- Intake 5xx 발생 여부: 발생하면 DB or 애플리케이션 자체 과부하

**서버 메트릭으로 확인하는 것:**
- Kafka consumer lag: intake 속도를 consumer가 따라가는가
- `executor.queued`: hirelogTaskExecutor 큐에 대기 중인 작업 수
- DLT 메시지 수: 몇 건이나 처리 실패로 빠졌는가

### 무엇을 알아내야 하는가 (최종 목표)

"초당 몇 건까지는 파이프라인이 정상 처리하고, 어느 시점에 Kafka lag 또는 executor 큐가 포화되는가"
→ 그 임계점을 수치로 찾는 것이 이 테스트의 목표다.

### 포폴 스토리 구조

```
[발견] 초당 50건 이상에서 Kafka consumer lag 지속 증가,
       executor queue 포화로 RejectedExecutionException 발생
[원인] consumer concurrency=3, executor maxPoolSize=16으로
       LLM 처리 속도가 intake 속도를 따라가지 못함
[개선] LLM 호출에 Semaphore 적용으로 동시 실행 수 제한 + graceful degradation
[결과] 초당 X건까지 안정 처리, DLT 발생 0건
```

---

## 실행

```bash
export BASE_URL=http://localhost:8080
export ACCESS_TOKEN=<JWT>

k6 run scenarios/A_search_load.js
k6 run scenarios/B_pipeline_throughput.js
```

## B 테스트 시 함께 모니터링

```bash
# Kafka consumer lag
kafka-consumer-groups.sh --bootstrap-server localhost:9092 \
  --describe --group <consumer-group>

# Executor 큐 (Spring Actuator)
curl http://localhost:8080/actuator/metrics/executor.queued
curl http://localhost:8080/actuator/metrics/executor.active

# DLT 메시지 발생 여부
kafka-console-consumer.sh --bootstrap-server localhost:9092 \
  --topic <topic>.DLT --from-beginning
```