# RAG Rate Limiting

## 구조

Rate limit은 두 레이어로 구성됨.

---

## 1. 애플리케이션 레벨 — Redis (RagRateLimiter)

- USER: 하루 3회 제한 (Redis INCR, 자정 TTL)
- ADMIN: 제한 없음
- key: `rag:rate:limit:{memberId}:{날짜}`
- 4회째 호출 시 `BusinessException(RAG_RATE_LIMIT_EXCEEDED)` → HTTP 429

---

## 2. Gemini API 레벨 — Resilience4j (geminiRateLimiter)

설정 (`application.yml`):
```yaml
gemini:
  limitForPeriod: 30
  limitRefreshPeriod: 1m
  timeoutDuration: 0s  # 즉시 실패
```

**Parser에만 적용. Parser 통과 후 후속 호출은 rate limit 없음.**

| 어댑터 | rate limit | 이유 |
|---|---|---|
| `GeminiRagParserAdapter` | ✅ 적용 | RAG 파이프라인 진입 제어 |
| `GeminiRagComposerAdapter` | ❌ 없음 | Parser 통과 시 완료 보장 |
| `GeminiRagFeatureExtractorAdapter` | ❌ 없음 | Parser 통과 시 완료 보장 |
| `GeminiJobSummaryLlm` | ✅ 적용 | 별도 JD 요약 플로우 |

### 설계 이유

Parser 성공 후 Composer에서 rate limit이 걸리면:
- Parser 결과 버려짐
- 유저 일일 쿼터 소모됨
- 사용자는 fallback 에러 메시지만 받음

→ Parser를 gate로 삼아 통과하면 끝까지 실행 보장.

---

## Rate Limit 걸리는 최종 조건

| 케이스 | 조건 | 결과 |
|---|---|---|
| USER 일일 초과 | 하루 4회째 `/api/rag/query` | HTTP 429 |
| Gemini 분당 초과 | 1분 내 RAG 진입 30회 초과 | Parser fallback (`DOCUMENT_SEARCH`) |
| Circuit Breaker | 10회 중 50% 실패 | 60초 오픈 상태 |