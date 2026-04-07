/**
 * 시나리오 A: OpenSearch 검색 부하 테스트
 *
 * 측정 포인트:
 *   JobSummaryReadService.search()
 *   - 비로그인: OpenSearch 쿼리 1회
 *   - 로그인:   OpenSearch 쿼리 + DB 쿼리(MemberJobSummary saved states) → 2회
 *
 * 목표:
 *   1. VU 증가에 따른 p99 latency 변화 측정 (baseline)
 *   2. 비로그인 vs 로그인 응답 시간 차이 → DB enrichment 비용 수치화
 *   3. Redis 캐시 적용 후 before/after 비교
 *
 * 실행:
 *   BASE_URL=http://localhost:8080 ACCESS_TOKEN=<JWT> k6 run A_search_load.js
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const ACCESS_TOKEN = __ENV.ACCESS_TOKEN || '';

const latencyAnon = new Trend('latency_anonymous_ms');    // 비로그인
const latencyAuth = new Trend('latency_authenticated_ms'); // 로그인
const errorRate = new Rate('error_rate');

export const options = {
  stages: [
    { duration: '30s', target: 10 },   // 워밍업
    { duration: '60s', target: 50 },   // 중간 부하
    { duration: '60s', target: 100 },  // 고부하
    { duration: '60s', target: 200 },  // 피크 — OpenSearch 한계 탐색
    { duration: '30s', target: 0 },
  ],
  thresholds: {
    'latency_anonymous_ms': ['p(99)<3000'],
    'latency_authenticated_ms': ['p(99)<3000'],
    'error_rate': ['rate<0.05'],
  },
};

// 실제 서비스에서 발생할 법한 검색 패턴
const SEARCH_CASES = [
  { keyword: '백엔드' },
  { keyword: '프론트엔드' },
  { keyword: 'Kotlin' },
  { keyword: 'React' },
  { keyword: null, careerType: 'EXPERIENCED' },
  { keyword: '데이터' },
  { keyword: 'DevOps' },
  { keyword: null },  // 전체 조회
];

function buildQueryString(params) {
  return Object.entries(params)
    .filter(([, v]) => v !== null && v !== undefined)
    .map(([k, v]) => `${k}=${encodeURIComponent(v)}`)
    .join('&');
}

export default function () {
  const searchCase = SEARCH_CASES[Math.floor(Math.random() * SEARCH_CASES.length)];
  const qs = buildQueryString({ ...searchCase, size: 20 });
  const url = `${BASE_URL}/api/job-summary/search?${qs}`;

  // 비로그인 검색 (OpenSearch만)
  const anonRes = http.get(url, { timeout: '10s' });
  latencyAnon.add(anonRes.timings.duration);
  const anonOk = check(anonRes, { 'anon search 200': (r) => r.status === 200 });
  errorRate.add(!anonOk);

  // 로그인 검색 (OpenSearch + DB enrichment)
  if (ACCESS_TOKEN) {
    const authRes = http.get(url, {
      headers: { 'Authorization': `Bearer ${ACCESS_TOKEN}` },
      timeout: '10s',
    });
    latencyAuth.add(authRes.timings.duration);
    const authOk = check(authRes, { 'auth search 200': (r) => r.status === 200 });
    errorRate.add(!authOk);
  }

  sleep(Math.random() * 0.5 + 0.2);
}

export function handleSummary(data) {
  const anon = data.metrics.latency_anonymous_ms?.values;
  const auth = data.metrics.latency_authenticated_ms?.values;

  const diff50 = anon && auth ? (auth['p(50)'] - anon['p(50)']).toFixed(0) : 'N/A';
  const diff99 = anon && auth ? (auth['p(99)'] - anon['p(99)']).toFixed(0) : 'N/A';

  return {
    'results/A_search_load_summary.json': JSON.stringify(data, null, 2),
    stdout: `
========================================
  시나리오 A: OpenSearch 검색 부하 테스트
========================================
  [비로그인 - OpenSearch 단독]
  p50 : ${anon?.['p(50)']?.toFixed(0) ?? 'N/A'} ms
  p95 : ${anon?.['p(95)']?.toFixed(0) ?? 'N/A'} ms
  p99 : ${anon?.['p(99)']?.toFixed(0) ?? 'N/A'} ms

  [로그인 - OpenSearch + DB enrichment]
  p50 : ${auth?.['p(50)']?.toFixed(0) ?? 'N/A'} ms
  p95 : ${auth?.['p(95)']?.toFixed(0) ?? 'N/A'} ms
  p99 : ${auth?.['p(99)']?.toFixed(0) ?? 'N/A'} ms

  [DB enrichment 추가 비용]
  p50 +${diff50} ms / p99 +${diff99} ms

  Error Rate: ${((data.metrics.error_rate?.values?.rate ?? 0) * 100).toFixed(2)} %
========================================
  [개선 방향]
  DB enrichment 비용이 크다면:
  → Redis에 검색 결과 캐싱 (TTL 60s)
  → 로그인 사용자 saved states IN-query 최적화
========================================
`,
  };
}