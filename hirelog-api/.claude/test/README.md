# HireLog 부하 테스트 설계 문서

## 목차

1. [테스트 전략 개요](#1-테스트-전략-개요)
2. [시나리오 A: OpenSearch 검색 부하 테스트](#2-시나리오-a-opensearch-검색-부하-테스트)
3. [시나리오 B: JD 파이프라인 처리량 테스트](#3-시나리오-b-jd-파이프라인-처리량-테스트)
4. [시드 데이터 설계](#4-시드-데이터-설계)
5. [결과 해석 가이드](#5-결과-해석-가이드)
6. [실행 방법](#6-실행-방법)

---

## 1. 테스트 전략 개요

### 왜 이 두 가지 시나리오인가

HireLog 백엔드에서 성능 위험이 실제로 존재하는 지점은 두 군데다.

```
[동기 경로]
사용자 요청 → GET /api/job-summary/search → OpenSearch + (로그인 시) DB 조회 → 응답
                                               ↑
                                   트래픽이 몰리면 latency가 직접 증가

[비동기 경로]
사용자 요청 → POST /api/job-summary/text → 즉시 200
                → Outbox → Kafka → preprocess-pipeline → LLM → OpenSearch
                                    ↑
                        intake는 빠르지만 파이프라인은 느림 → 적체 발생
```

나머지 API(게시판 CRUD, 알림 등)는 단순 DB 조회/쓰기라 병목이 명확하지 않다.
성능 테스트는 "전체를 다 테스트"하는 것이 아니라 **실제 병목이 발생할 가능성이 높은 곳**을 정밀하게 측정하는 것이 목적이다.

### 측정 지표 선택 이유

| 지표 | 선택 이유 |
|---|---|
| p50 | 일반 사용자 경험의 중앙값. 이 값이 높으면 대다수 사용자가 느리다. |
| p95 | SLO 기준으로 자주 사용. 상위 5% 느린 요청까지 포함. |
| p99 | 최악의 케이스. 파워 유저, 복잡한 쿼리 등이 여기에 걸린다. |
| Error Rate | 5% 초과 시 시스템이 감당 불가 상태. cascade failure 신호. |

p99만 보면 충분하지 않은가? → 아니다. **p50이 낮고 p99가 높으면** 특정 조건(대형 IN-query, 캐시 미스)에서만 느린 것이고, **p50과 p99가 함께 높으면** 근본적인 처리량 한계다. 두 지표를 같이 봐야 원인이 분리된다.

---

## 2. 시나리오 A: OpenSearch 검색 부하 테스트

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

### 왜 비로그인과 로그인을 분리해서 측정하는가

```
로그인 latency - 비로그인 latency = DB enrichment 추가 비용 (ms)
```

이 값이 크면 다음 중 하나가 문제다:
- `member_job_summary` 인덱스 미흡 → 풀스캔 발생
- HikariCP connection pool 고갈 → DB 대기 발생
- IN-query 크기 폭발 (파워 유저의 수백 건 저장 상태)

두 개를 합쳐서 측정하면 "검색이 느리다"는 사실만 알 수 있고, 원인이 OpenSearch인지 DB인지 분리할 수 없다.

### VU 단계 설계 의도

```
30s  → 10 VU  (워밍업: JVM JIT 컴파일, OpenSearch warm cache)
60s  → 50 VU  (중간 부하: 일반 운영 트래픽 수준)
60s  → 100 VU (고부하: 이벤트/공지 직후 트래픽 급증 시뮬레이션)
60s  → 200 VU (피크: OpenSearch thread pool 포화 지점 탐색)
30s  → 0 VU   (쿨다운)
```

워밍업 30초를 두는 이유: JVM은 첫 요청 시 JIT 컴파일이 발생하며 latency가 튄다. 워밍업 없이 측정하면 초기 spike가 p99를 오염시킨다.

200 VU까지 올리는 이유: OpenSearch의 search thread pool 기본 크기는 `(CPU core 수 × 3 / 2 + 1)`이다. 8코어 기준 13개. VU가 thread pool을 초과하면 queue에 쌓이고 latency가 급등한다. **그 임계점이 몇 VU인지**를 찾는 것이 피크 단계의 목적이다.

### 검색 패턴 설계 의도

```javascript
const SEARCH_CASES = [
  { keyword: '백엔드' },     // 검색 집중 키워드 — hot term cache 경합 재현
  { keyword: 'Kotlin' },
  { keyword: null },         // 전체 조회 — 결과 많아서 scoring 비용 높음
  { keyword: null, careerType: 'EXPERIENCED' }, // 필터 + 전체 조회
  ...
];
```

실제 검색 트래픽은 균일하지 않다. 특정 키워드에 집중된다. 균일 분포로 테스트하면 OpenSearch의 hot term caching 효과가 과대평가되고, 실제 운영에서 발견되는 병목을 재현하지 못한다.

`keyword: null`(전체 조회)을 포함한 이유: 전체 조회는 결과 건수가 많아 OpenSearch의 score 계산·정렬 비용이 크다. 이 케이스가 섞여야 현실적인 평균 latency가 나온다.

### 무엇을 알아내야 하는가

1. **VU 증가에 따른 latency 변화**: p50/p95/p99가 어느 VU 구간에서 급등하는가
2. **비로그인 vs 로그인 latency 차이**: DB enrichment 추가 비용이 실제로 얼마인가 (ms 단위로)
3. **OpenSearch 한계**: 어느 시점에서 OpenSearch가 병목이 되는가 (CPU, heap 급등)
4. **개선 효과**: Redis 캐시 적용 후 p99가 얼마나 줄어드는가

### 포트폴리오 스토리 구조

```
[발견] VU 200 기준 로그인 검색 p99 = Xms, 비로그인 대비 Yms 더 느림
        → p50 차이는 Zms인데 p99는 Wms 차이 → 분산 높음 → IN-query 크기 불균형
[원인] 매 검색마다 MemberJobSummary DB 조회 발생
        파워 유저(500건+ 저장)의 IN-query가 일반 사용자의 20배 크기
[개선] 검색 결과 Redis 캐싱 (TTL 60s) or saved states IN-query 최적화
[결과] p99 X'ms로 감소, 캐시 히트율 Z%
```

---

## 3. 시나리오 B: JD 파이프라인 처리량 테스트

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

### 처리량 임계점 탐색 방법

```
낮은 rps부터 점진적으로 증가
→ Kafka consumer lag이 수렴하지 않고 계속 증가하는 지점 = 임계점

임계점 이하: lag이 처리되며 수렴 (시스템이 따라감)
임계점 이상: lag이 선형 또는 지수적으로 증가 (시스템이 따라가지 못함)
```

최대 처리량 이론값 계산:
```
consumer concurrency × (1000ms / LLM 평균 응답 시간ms) × executor maxPoolSize
= 3 × (1000ms / 2000ms) × 16 ≈ 24 req/s
```
이 값과 실측 임계점의 차이를 비교하면 어디서 추가 병목이 발생하는지 확인할 수 있다.

### 최종 목표

"초당 몇 건까지는 파이프라인이 정상 처리하고, 어느 시점에 Kafka lag 또는 executor 큐가 포화되는가"
→ 그 임계점을 수치로 찾는 것이 이 테스트의 목표다.

### 포트폴리오 스토리 구조

```
[발견] 초당 50건 이상에서 Kafka consumer lag 지속 증가,
       executor queue 포화로 RejectedExecutionException 발생
[원인] consumer concurrency=3, executor maxPoolSize=16으로
       LLM 처리 속도가 intake 속도를 따라가지 못함
[개선] LLM 호출에 Semaphore 적용으로 동시 실행 수 제한 + graceful degradation
[결과] 초당 X건까지 안정 처리, DLT 발생 0건
```

---

## 4. 시드 데이터 설계

### 왜 100만 건인가

| 문서 수 | 문제점 |
|---|---|
| 10,000건 | OpenSearch segment가 메모리에 전부 올라감 → 실제 디스크 I/O 미발생 → 비현실적으로 빠름 |
| 100,000건 | 충분한 volume이지만 hot keyword hit 집중이 약함 |
| **1,000,000건** | segment 분산으로 실제 디스크 I/O + cache eviction 발생. scoring 계산 비용이 현실적. |

100만 건에서 "백엔드" 검색 시 OpenSearch가 처리해야 하는 posting list 크기가 실제 서비스 수준에 가까워진다.

### Hot keyword 편중 설계

실제 검색 트래픽의 특성:
- 상위 3개 키워드("백엔드", "Spring", "Kotlin")가 전체 검색의 약 60% 차지
- 이 키워드들을 포함한 문서에 검색이 집중 → OpenSearch의 해당 term cache가 경합

시드 데이터 구현:
- 전체 문서의 30%에 hot keyword를 `summaryText` + `responsibilities` + `techStack` 3개 필드에 동시 주입
- 단순히 한 필드에만 넣으면 TF-IDF score가 낮아 검색 결과 상위에 반영되지 않음
- 3개 필드 주입 → relevance score 상승 → 실제 서비스와 동일한 scoring 부하 재현

### 최신 데이터 편중 설계

```
최근 7일:   40% (활발한 채용 시즌 공고 집중)
최근 30일:  35%
최근 90일:  20%
최근 180일:  5%
```

균일 분포로 시드를 구성하면 `createdAt` 기준 정렬·필터 쿼리의 selectivity가 과도하게 낮아져 OpenSearch score 계산이 비현실적으로 단순해진다.

### DB enrichment 병목 시나리오 설계

Power user 패턴 — IN-query 크기 불균형 재현:

```
Light user (60%): 평균의 40% 저장 (~30건) — 소형 IN-query, DB 부하 낮음
Medium user (29%): 평균 수준 저장 (~80건) — 일반
Heavy user (10%): 평균의 2.5배 저장 (~200건)
Power user  (1%): 평균의 15배 저장 (500~2000건) — IN-query 폭발
```

Hot job 패턴 — DB buffer pool 경합 재현:
```
전체 job 중 상위 0.1%를 "hot job"으로 선정
→ 전체 member의 80%가 이 hot job을 저장한 상태로 시드 구성
→ 동시에 여러 member의 IN-query에 동일한 row가 포함됨
→ DB buffer pool에서 동일 page를 경쟁적으로 읽는 패턴 재현
```

### 스트리밍 bulk 설계

```
100만 건 삽입 시 all_docs: list[dict] 전체 수집 → 약 2~3GB 메모리 점유
generator를 helpers.bulk()에 직접 전달 → O(chunk_size) 고정

bulk 중 refresh_interval = -1 설정 이유:
- OpenSearch는 기본 1초마다 segment refresh 수행
- 100만 건 삽입 중 refresh가 계속 발생하면 삽입 처리량 약 40% 감소
- bulk 완료 후 force merge + refresh_interval 복구로 검색 성능 안정화
```

---

## 5. 결과 해석 가이드

### p50↔p99 해석

| 케이스 | p50 | p99 | 의미 |
|---|---|---|---|
| 정상 | 80ms | 180ms | 안정적 분산 |
| 특정 조건 느림 | 90ms | 1200ms | 파워 유저 IN-query 또는 캐시 미스 |
| 전반적 과부하 | 400ms | 900ms | OpenSearch thread pool 포화 |
| 커넥션 대기 | 200ms | 4000ms | HikariCP pool 고갈 |

### 시나리오 A 판독 기준

```
정상:   p50/p99 간격 < 2배, 로그인 - 비로그인 차이 < 50ms
주의:   p99가 VU 100 이상에서 급등 → OpenSearch thread pool 포화 지점
        로그인 - 비로그인 p99 차이 > 200ms → IN-query 최적화 필요
위험:   Error Rate > 1%, p99 > 3000ms
```

### 개선 전/후 비교 방법

```bash
# before 측정 후 결과 보존
cp results/A_search_load_summary.json results/A_before.json

# 개선 적용 후 재측정
k6 run scenarios/A_search_load.js

# 비교: p99 절대값, 로그인-비로그인 차이, Error Rate 변화 확인
```

---

## 6. 실행 방법

### 시드 데이터 삽입

```bash
cd .claude/test/seed
chmod +x run_seed.sh
./run_seed.sh
# → 인터랙티브 메뉴: 모드 선택(1~5) → 문서 수 선택(1만/10만/50만/100만/직접입력)
```

### k6 테스트 실행

```bash
# A: 검색 부하 테스트
cd .claude/test
k6 run scenarios/A_search_load.js

# B: 파이프라인 처리량 테스트
k6 run scenarios/B_pipeline_throughput.js
```

결과 JSON: `results/A_search_load_summary.json`

### B 테스트 시 함께 모니터링

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

### 접속 설정 오버라이드

```bash
OS_HOST=localhost OS_PORT=9200 OS_USER=admin OS_PASSWORD=admin \
PG_HOST=localhost PG_PORT=5432 PG_DB=hirelog PG_USER=postgres PG_PASSWORD=postgres \
JOB_COUNT=1000000 MEMBER_COUNT=1000 MEMBER_JOB_SUMMARY_COUNT=80000 \
./run_seed.sh

# k6 환경변수
BASE_URL=http://localhost:8080 ACCESS_TOKEN=<JWT> k6 run scenarios/A_search_load.js
```

### 적재 후 확인

```bash
# OpenSearch 문서 수 확인
curl -u admin:admin http://localhost:9200/job_summary/_count

# OpenSearch 클러스터 상태
curl -u admin:admin http://localhost:9200/_cluster/health?pretty
```