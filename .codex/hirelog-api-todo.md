# HireLog API TODO

작성일: 2026-03-27
대상: `hirelog-api`

## 장애/기능 이슈 (우선)

- [x] 리뷰 관리/리뷰 목록 조회 불가 원인 파악 및 수정
- [x] 상세페이지 준비기록 저장 실패 수정 (서류전형, 코딩테스트)
- [x] 관리자 채용공고 상태 전환 버그 수정 (비활성 -> 활성 불가)
- [x] 내가 저장한 채용공고 목록 조회 불가 수정
- [x] 알림 조회 API 동작/응답 스키마 점검 및 수정

## 관리자 분리

- [x] JD 요약 요청 관리자 전용 API 분리
- [x] 관리자 권한 검증 미들웨어/가드 점검

## 고도화

- [ ] [P1] 재처리/복구 시스템 정교화 (DLT, replay-safe, resume, 운영툴)
- [ ] [P2] 검색 품질 개선 (OpenSearch relevance, analyzer, autocomplete/typo, 로그 기반 튜닝)
- [ ] [P3] 데이터 정규화/중복 제거 고도화 (canonical model, dedupe 정밀도)
- [ ] [P4] CQRS Read 모델 분리/최적화 (projection, rebuild, consistency lag)
- [ ] [P5] 관측 가능성 강화 (correlation ID, per-stage metrics, tracing, alert/SLO)
- [ ] [P6] LLM 운영 안정화 (prompt versioning, quality gate, fallback, semantic cache, cost control)
- [ ] [P7] 성능 실험 리포트화 (partition/concurrency/batch 실험, 전후 지표 비교)
- [ ] [P8] 봇 트래픽 + 장애 주입(Chaos) 기반 장애 대응/복구 훈련 자동화
- [ ] [P9] Python 워커 CPU 병목 분석 및 Rust/Go 기반 워커 전환 검토
- [ ] [P10] RAG 파이프라인 설계/구현
- [ ] [P11] Spring Kotlin 코드 재설계 (헥사고날 아키텍처 + CQRS 적용)
- [ ] [P12] Swagger(OpenAPI) 자동 생성/배포 및 API 계약 관리 체계화

## 네카라쿠배 대비 우선순위 (고도화만)

1. `재처리/복구 시스템 정교화`
2. `검색 품질 개선`
3. `데이터 정규화/중복 제거 고도화`
4. `CQRS Read 모델 분리/최적화`
5. `관측 가능성 강화`
6. `LLM 운영 안정화`
7. `성능 실험 리포트화`

## 지금 당장 집중할 3개 (현실 버전)

- [ ] 1. 재처리/복구 시스템 정교화
- [ ] 2. 검색 품질 개선
- [ ] 3. 관측 가능성 강화

## 1순위 상세: 재처리/복구 시스템 (면접 핵심)

- [ ] partition key 설계 근거 문서화 (`job_source_id` 또는 `dedupe_key`)
- [ ] consumer rebalance/재시작 시 중복 실행 방지 (`idempotency key` + DB unique constraint)
- [ ] `processed_event(event_id unique)` 기반 side effect 차단
- [ ] retry(즉시 재시도) vs replay(운영 재주입) 절차 분리
- [ ] poison message 격리(DLT) + 운영자 재주입 도구 제공
- [ ] out-of-order 허용 여부 결정 및 보정 규칙 문서화

## 2순위 상세: 검색 품질 개선

- [ ] 인덱스 필드 목적 분리(exact / analyzed / autocomplete / ranking feature)
- [ ] relevance 튜닝(function_score, field boost, freshness 가중치)
- [ ] synonym/typo tolerance 적용
- [ ] 검색 품질 지표 운영(CTR, zero-result rate, top-k 샘플 평가)

## 3순위 상세: 관측 가능성 강화

- [ ] requestId/jobId end-to-end 전파(HTTP -> Kafka -> Worker -> DB -> OpenSearch)
- [ ] 단계별 지연 메트릭(ingest/preprocess/llm/persist/index)
- [ ] SLI/SLO 및 Alert rule 정의
- [ ] 실패 메시지 조회/재처리/Admin recovery API 제공

## P8 상세: 장애 유도 부하 테스트 (Chaos + Load)

- [ ] 봇 트래픽 시나리오 작성 (정상/스파이크/장기 부하)
- [ ] 장애 주입 시나리오 정의 (Kafka 지연, consumer down, DB 지연, LLM timeout)
- [ ] 장애 탐지 목표시간(MTTD), 복구시간(MTTR), 데이터 유실 허용치(RPO) 정의
- [ ] 실패 단계별 자동 대응(runbook + admin tooling) 검증
- [ ] DLT 적재 -> 재주입(replay) -> 정합성 검증까지 E2E 리허설
- [ ] 실험 결과 리포트화 (원인, 대응, 재발방지, 아키텍처 개선사항)

## 운영 안정성 체크리스트

- [ ] DLT 적재 실패/유실/지연 모니터링 지표 확인
- [ ] 장애 발생 시 복구 런북 존재 여부 및 실제 복구 리허설
- [ ] 스케줄러 실패 재시도 정책/데드레터큐(DLQ) 점검
- [ ] 배치 실패 알림(Slack/Email/Webhook) 점검

## Python 워커 성능 개선

- [ ] CPU 핫패스 프로파일링(py-spy/cProfile/OTel)으로 병목 함수 식별
- [ ] Rust/Go 전환 대상 선정(파싱, 임베딩 전처리, 대량 변환 등 CPU bound 구간)
- [ ] 기존 Python 워커 대비 처리량/지연시간/비용 비교 벤치마크
- [ ] 단계적 전환 전략 수립(사이드카 또는 마이크로서비스 형태)

## OpenSearch + Read 모델 최적화

- [ ] 조회 패턴 기준 Read 모델(Projection/Denormalization) 재설계
- [ ] OpenSearch 인덱스 매핑/Analyzer/필드 전략 재정의
- [ ] 쿼리 튜닝(필터 우선, 캐시 전략, 페이징, 스코어링) 및 P95 응답시간 개선
- [ ] API 응답 DTO와 Read 인덱스 동기화 정책(실시간/지연 허용) 정리

## Spring Kotlin 재설계 (헥사고날 + CQRS)

- [ ] Domain/Application/Adapter 경계 재정의 및 패키지 구조 정리
- [ ] 쓰기 모델(Command)과 조회 모델(Query) 분리
- [ ] Port/Adapter 기준 명확화(외부 의존성만 Port로 격리)
- [ ] 트랜잭션 경계 Application Service 일원화
- [ ] Projection rebuild/consistency lag 측정 전략 반영

## Swagger(OpenAPI) 생성

- [ ] springdoc-openapi 적용 및 환경별 문서 노출 정책 정의
- [ ] 관리자/일반 사용자 API 태그 분리
- [ ] 인증/에러 응답 스키마 표준화
- [ ] CI에서 OpenAPI 스펙 생성/검증 자동화

## 통합 테스트 코드 구축

- [ ] SpringBootTest + Testcontainers 기반 통합 테스트 환경 구성
- [ ] 핵심 사용자 시나리오 통합 테스트 (저장/조회/수정/상태전이)
- [ ] 관리자 시나리오 통합 테스트 (권한 검증, 관리자 전용 API)
- [ ] 이벤트 파이프라인 통합 테스트 (produce -> consume -> 저장/색인 결과 검증)
- [ ] 장애/재처리 시나리오 테스트 (DLT, replay, idempotency 보장)

## 연계 확인 포인트

- [ ] `hirelog-web`에서 사용하는 요청/응답 필드와 API 계약 정합성 점검
