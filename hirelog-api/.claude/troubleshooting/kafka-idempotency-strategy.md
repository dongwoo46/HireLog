# Kafka 멱등성 전략 — 컨슈머별 정책 분리

## 문제

`JdPreprocessFailConsumer`가 두 단계 멱등 체크(SELECT → INSERT)를 사용했는데
`JdPreprocessResponseConsumer`는 단일 INSERT-first를 사용해 전략이 혼재됨.

두 단계 방식의 race condition:
```
인스턴스 A: isAlreadyProcessed() → false
인스턴스 B: isAlreadyProcessed() → false   ← race window
인스턴스 A: handle() 실행
인스턴스 B: handle() 실행   ← 중복 실행 (알림 2번 발송)
인스턴스 A: markProcessed() → INSERT 성공
인스턴스 B: markProcessed() → duplicate (비즈니스 로직은 이미 실행됨)
```

## INSERT-first 방식의 문제 (at-most-once)

```
1. isAlreadyProcessedOrMark() → TX1 커밋 (mark INSERT)
2. handle() 실패 → TX2 롤백
3. Kafka 재시도
4. isAlreadyProcessedOrMark() → duplicate → true → skip
   ⟹ 비즈니스 로직 영구 미처리 (알림 누락)
```

## 결론: 컨슈머별 정책 의도적 분리

| 컨슈머 | 전략 | 이유 |
|---|---|---|
| `JdPreprocessResponseConsumer` | INSERT-first (at-most-once) | 중복 LLM 호출 방지 우선 (비용, 중복 JobSummary 생성) |
| `JdPreprocessFailConsumer` | SELECT-then-INSERT (at-least-once) | **알림 누락 방지 우선** — 중복 알림 < 알림 누락 |

## JdPreprocessFailConsumer 현재 구조

```kotlin
// 1. SELECT (중복 체크)
val alreadyProcessed = processedEventService.isAlreadyProcessed(eventId, consumerGroup)
if (alreadyProcessed) { ack; return }

// 2. 비즈니스 로직
jdPreprocessFailHandler.handle(event)  // 실패 시 throw → Kafka 재시도

// 3. INSERT (성공 후 마킹)
processedEventService.markProcessed(eventId, consumerGroup)

// 4. ack
acknowledgment.acknowledge()
```

handle() 실패 시 markProcessed가 호출되지 않아 재시도 가능 → at-least-once 보장.

## race condition 허용 근거

- 멀티 인스턴스에서 동일 fail 이벤트가 동시 처리될 가능성이 극히 낮음
- 발생 시 영향: 중복 실패 알림 1회 (허용 범위)
- 대안(완전한 at-least-once without race): mark를 handle TX 안으로 이동 필요
  → PostgreSQL constraint violation이 outer TX를 abort 시키는 문제로
     `INSERT ... ON CONFLICT DO NOTHING` DB 레벨 변경이 전제됨 → 현재 미적용

## 완전한 해결책 (미적용, 참고용)

mark와 비즈니스 로직을 같은 트랜잭션으로 묶으면 race condition 없이 at-least-once 달성 가능.
전제 조건: `ProcessedEventService.markProcessed()`를 `INSERT ... ON CONFLICT DO NOTHING`으로 교체.
이 경우 duplicate INSERT가 예외 없이 처리되어 outer TX 오염 없음.