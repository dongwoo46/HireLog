/**
 * 시나리오 B: JD 파이프라인 처리량 테스트
 *
 * 구조:
 *   k6 → POST /api/job-summary/text (즉시 200 반환, 비동기)
 *        → Outbox → Debezium → Kafka → preprocess-pipeline
 *        → Kafka response → JdSummaryGenerationFacade
 *        → thenAcceptAsync(..., hirelogTaskExecutor)  ← core=8, max=16, queue=200
 *        → LLM 호출 → OpenSearch 인덱싱
 *
 * 핵심 질문:
 *   "초당 N건을 intake에 밀어 넣으면 파이프라인이 감당하는가?"
 *
 * k6가 측정하는 것:
 *   - Intake API 응답 시간 (항상 빠름 — 이상하면 DB 문제)
 *   - Intake 수용 error rate (5xx 발생 시 DB or 커넥션 문제)
 *
 * k6 외부에서 함께 봐야 하는 것: (README 참고)
 *   - Kafka consumer lag → 파이프라인이 쌓이는지
 *   - executor.queued  → hirelogTaskExecutor 큐 적체
 *   - DLT 메시지 수    → 처리 실패 건수
 *   - LLM 호출 실패 로그
 *
 * 실행:
 *   BASE_URL=http://localhost:8080 ACCESS_TOKEN=<JWT> k6 run B_pipeline_throughput.js
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Counter, Trend } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const ACCESS_TOKEN = __ENV.ACCESS_TOKEN || '';

const intakeErrorRate = new Rate('intake_error_rate');
const intakeLatency = new Trend('intake_latency_ms');
const totalSubmitted = new Counter('total_submitted');

export const options = {
  /**
   * 단계별 처리량 증가
   *
   * hirelogTaskExecutor: core=8, max=16, queue=200
   * Kafka consumer concurrency: 3
   * → 이 설정 기준으로 포화 지점 탐색
   *
   * 각 stage에서 Kafka lag과 executor.queued를 함께 관찰할 것
   */
  stages: [
    { duration: '60s', target: 5 },   // 5 req/s — 정상 처리 확인 (baseline)
    { duration: '60s', target: 20 },  // 20 req/s — executor 큐 누적 시작 지점
    { duration: '60s', target: 50 },  // 50 req/s — Kafka lag 적체 구간
    { duration: '60s', target: 100 }, // 100 req/s — 파이프라인 포화
    { duration: '30s', target: 0 },
  ],
  thresholds: {
    // Intake API 자체가 느려지면 (>1s) DB or 커넥션 문제
    'intake_latency_ms': ['p(95)<1000'],
    'intake_error_rate': ['rate<0.01'],
  },
};

const JD_SAMPLES = [
  {
    brandName: '카카오',
    brandPositionName: '백엔드 엔지니어',
    jdText: `[백엔드 엔지니어 채용]
자격요건: Java/Kotlin 3년 이상, Spring Boot, JPA
우대사항: Kafka, Redis, MSA 경험
담당업무: 대규모 트래픽 처리 서비스 개발`,
  },
  {
    brandName: '네이버',
    brandPositionName: '프론트엔드 개발자',
    jdText: `[프론트엔드 개발자 채용]
자격요건: React, TypeScript 2년 이상
우대사항: Next.js, GraphQL
담당업무: 사용자 인터페이스 개발 및 성능 최적화`,
  },
  {
    brandName: '토스',
    brandPositionName: 'SRE 엔지니어',
    jdText: `[SRE 엔지니어 채용]
자격요건: Linux, Kubernetes, Prometheus 경험
우대사항: Terraform, ArgoCD
담당업무: 서비스 안정성 향상 및 배포 자동화`,
  },
];

export default function () {
  const sample = JD_SAMPLES[(__VU + __ITER) % JD_SAMPLES.length];

  const payload = JSON.stringify({
    ...sample,
    // VU/ITER 조합으로 고유 요청 생성 (중복 방지)
    brandPositionName: `${sample.brandPositionName}_${__VU}_${__ITER}`,
  });

  const start = Date.now();
  const res = http.post(
    `${BASE_URL}/api/job-summary/text`,
    payload,
    {
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${ACCESS_TOKEN}`,
      },
      timeout: '5s',
    }
  );

  intakeLatency.add(Date.now() - start);

  const ok = check(res, {
    'intake accepted': (r) => r.status === 200,
  });

  intakeErrorRate.add(!ok);
  if (ok) totalSubmitted.add(1);

  // 의도적으로 짧은 대기: 큐 적체를 빠르게 유도
  sleep(0.05);
}

export function handleSummary(data) {
  const lat = data.metrics.intake_latency_ms?.values;
  const errRate = data.metrics.intake_error_rate?.values?.rate ?? 0;
  const submitted = data.metrics.total_submitted?.values?.count ?? 0;
  const duration = data.state?.testRunDurationMs ?? 0;
  const rps = duration > 0 ? (submitted / (duration / 1000)).toFixed(1) : 'N/A';

  return {
    'results/B_pipeline_throughput_summary.json': JSON.stringify(data, null, 2),
    stdout: `
========================================
  시나리오 B: JD 파이프라인 처리량 테스트
========================================
  Intake API p50  : ${lat?.['p(50)']?.toFixed(0) ?? 'N/A'} ms
  Intake API p95  : ${lat?.['p(95)']?.toFixed(0) ?? 'N/A'} ms
  Intake API p99  : ${lat?.['p(99)']?.toFixed(0) ?? 'N/A'} ms
  Intake 평균 RPS : ${rps} req/s
  총 제출 건수    : ${submitted}
  Intake 에러율   : ${(errRate * 100).toFixed(2)} %
========================================
  [Intake API 판단 기준]
  p95 < 100ms  → DB 정상 (3 INSERT가 빠르게 처리됨)
  p95 > 500ms  → HikariCP 고갈 or DB 과부하 의심
  5xx 발생     → 즉시 서버 로그 확인 필요

  [파이프라인 포화는 외부 메트릭으로 확인]
  Kafka lag 증가   → consumer가 intake 속도를 못 따라감
  executor.queued  → hirelogTaskExecutor 큐 적체 (max=200)
  DLT 메시지 발생  → 처리 실패 (3회 재시도 후 포기)

  [개선 방향]
  - executor corePoolSize/maxPoolSize 조정
  - LLM 호출에 Semaphore로 동시 실행 수 제한
  - Kafka consumer concurrency 증설 (현재 partition 수와 맞춰야 함)
========================================
`,
  };
}