/**
 * 시나리오 A-complex: OpenSearch 복합 쿼리 부하 테스트
 *
 * A_search_load.js 와의 차이:
 *   A_search_load.js  → 단순 keyword/careerType 기본 검색. baseline 측정용.
 *   A_search_load_complex.js → 실제 서비스에서 발생하는 복합 쿼리 패턴.
 *                               쿼리 유형별 latency를 분리해 OpenSearch 어느 쿼리가 병목인지 식별.
 *
 * 시나리오 그룹 (6개):
 *   HOT_RELEVANCE  — hot keyword + RELEVANCE sort      → OpenSearch scoring 계산 최대 부하
 *   MULTI_FILTER   — keyword + 4종 필터 복합            → filter context 복합 쿼리 처리 비용
 *   LARGE_RESULT   — 전체 조회 size=100               → fetch phase / 직렬화 병목
 *   CURSOR_PAGE    — cursor 기반 연속 2페이지 요청      → Search After deep pagination 비용
 *   BRAND_FILTER   — brandId + positionCategoryId       → term filter 전용 (scoring 없음)
 *   TECH_STACK     — techStacks 정확 매칭               → keyword field 다중 term
 *
 * 각 그룹: 비로그인(OpenSearch만) + 로그인(+DB enrichment) 분리 측정
 *
 * 실행:
 *   BASE_URL=http://localhost:8080 ACCESS_TOKEN=<JWT> k6 run A_search_load_complex.js
 */

import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';

const BASE_URL    = __ENV.BASE_URL    || 'http://localhost:8080';
const ACCESS_TOKEN = __ENV.ACCESS_TOKEN || '';

// ── 그룹별 Trend 메트릭 ──────────────────────────────────────────────────────
// anon: OpenSearch만 / auth: OpenSearch + DB enrichment
const mHotAnon    = new Trend('latency_hot_relevance_anon_ms');
const mHotAuth    = new Trend('latency_hot_relevance_auth_ms');
const mMultiAnon  = new Trend('latency_multi_filter_anon_ms');
const mMultiAuth  = new Trend('latency_multi_filter_auth_ms');
const mLargeAnon  = new Trend('latency_large_result_anon_ms');
const mLargeAuth  = new Trend('latency_large_result_auth_ms');
const mCursorAnon = new Trend('latency_cursor_page_anon_ms');
const mCursorAuth = new Trend('latency_cursor_page_auth_ms');
const mBrandAnon  = new Trend('latency_brand_filter_anon_ms');
const mBrandAuth  = new Trend('latency_brand_filter_auth_ms');
const mTechAnon   = new Trend('latency_tech_stack_anon_ms');
const mTechAuth   = new Trend('latency_tech_stack_auth_ms');

const errorRate   = new Rate('error_rate');
const cursorMiss  = new Counter('cursor_extract_fail'); // cursor 파싱 실패 횟수

// ── 스테이지 설정 ────────────────────────────────────────────────────────────
// A_search_load.js 대비 peak VU 50% 상향 (300 VU)
// 복합 쿼리는 단순 검색보다 OpenSearch 부하가 크기 때문에 한계점이 더 낮게 나타날 수 있음
export const options = {
  stages: [
    { duration: '30s', target: 10  }, // 워밍업
    { duration: '60s', target: 50  }, // 중간 부하
    { duration: '60s', target: 150 }, // 고부하
    { duration: '60s', target: 300 }, // 피크 — 복합 쿼리 한계 탐색
    { duration: '30s', target: 0   }, // 쿨다운
  ],
  thresholds: {
    // 복합 쿼리이므로 임계값을 기본 테스트보다 완화 (5초)
    'latency_hot_relevance_anon_ms':   ['p(99)<5000'],
    'latency_hot_relevance_auth_ms':   ['p(99)<5000'],
    'latency_multi_filter_anon_ms':    ['p(99)<5000'],
    'latency_multi_filter_auth_ms':    ['p(99)<5000'],
    'latency_large_result_anon_ms':    ['p(99)<8000'], // size=100: 직렬화 비용 별도
    'latency_large_result_auth_ms':    ['p(99)<8000'],
    'latency_cursor_page_anon_ms':     ['p(99)<5000'],
    'latency_cursor_page_auth_ms':     ['p(99)<5000'],
    'latency_brand_filter_anon_ms':    ['p(99)<3000'], // ID filter: 가장 빠른 케이스
    'latency_brand_filter_auth_ms':    ['p(99)<3000'],
    'latency_tech_stack_anon_ms':      ['p(99)<5000'],
    'latency_tech_stack_auth_ms':      ['p(99)<5000'],
    'error_rate':                      ['rate<0.05'],
  },
  summaryTrendStats: ['p(50)', 'p(95)', 'p(99)'],
};

// ── 데이터 풀 ────────────────────────────────────────────────────────────────

// 검색 집중 hot keyword (seed 데이터와 일치)
const HOT_KEYWORDS = ['백엔드', 'Spring', 'Kotlin', 'Java', 'Redis', 'Kafka'];

// 일반 keyword
const GENERAL_KEYWORDS = ['프론트엔드', 'DevOps', 'Python', 'AWS', '데이터', 'SRE', 'ML'];

// positionCategoryId: seed 데이터 기준 (1~6)
const POSITION_CATEGORY_IDS = [1, 2, 3, 4, 5, 6]; // 서버/백엔드, 웹 프론트엔드, 데이터, 인프라/DevOps, 모바일, 보안

// brandId: seed 데이터 기준 (1~10)
const BRAND_IDS = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10];

// techStack 조합 (seed의 TECH_STACK_GROUPS에 대응)
const TECH_STACK_COMBOS = [
  ['Kotlin', 'Spring Boot'],
  ['Java', 'Spring Boot'],
  ['Python', 'FastAPI'],
  ['TypeScript', 'React'],
  ['Go', 'Kubernetes'],
  ['Terraform', 'AWS'],
  ['Java', 'Kafka'],
  ['Python', 'Airflow'],
];

const SORT_OPTIONS = ['CREATED_AT_DESC', 'CREATED_AT_ASC', 'RELEVANCE'];

// ── 유틸 ────────────────────────────────────────────────────────────────────

function pick(arr) {
  return arr[Math.floor(Math.random() * arr.length)];
}

/**
 * 쿼리 파라미터 빌더
 * - null/undefined 제거
 * - 배열은 key=v1&key=v2 형식으로 직렬화 (Spring List<String> 바인딩 호환)
 */
function buildQS(params) {
  const parts = [];
  for (const [k, v] of Object.entries(params)) {
    if (v === null || v === undefined) continue;
    if (Array.isArray(v)) {
      for (const item of v) {
        parts.push(`${k}=${encodeURIComponent(item)}`);
      }
    } else {
      parts.push(`${k}=${encodeURIComponent(v)}`);
    }
  }
  return parts.join('&');
}

/** 응답 body에서 nextCursor 추출. 실패 시 null 반환. */
function extractCursor(res) {
  try {
    const body = JSON.parse(res.body);
    return body.nextCursor ?? null;
  } catch (_) {
    return null;
  }
}

/** GET 요청 + check + metric 기록 */
function doGet(url, headers, metric) {
  const res = http.get(url, { headers, timeout: '15s' });
  metric.add(res.timings.duration);
  const ok = check(res, { 'status 200': (r) => r.status === 200 });
  errorRate.add(!ok);
  return res;
}

const AUTH_HEADERS = ACCESS_TOKEN
  ? { Authorization: `Bearer ${ACCESS_TOKEN}` }
  : {};

// ── 시나리오 그룹 함수 ────────────────────────────────────────────────────────

/**
 * GROUP 1: HOT_RELEVANCE
 * hot keyword + RELEVANCE sort
 * → OpenSearch best_fields scoring + 관련도 정렬 비용 최대화
 * → seed에서 30% 문서에 hot keyword를 3개 필드에 주입했으므로 scoring 계산 문서 수 多
 */
function runHotRelevance() {
  const keyword = pick(HOT_KEYWORDS);
  const qs = buildQS({ keyword, sortBy: 'RELEVANCE', size: 20 });
  const url = `${BASE_URL}/api/job-summary/search?${qs}`;

  group('HOT_RELEVANCE', () => {
    doGet(url, {}, mHotAnon);
    if (ACCESS_TOKEN) doGet(url, AUTH_HEADERS, mHotAuth);
  });
}

/**
 * GROUP 2: MULTI_FILTER
 * keyword + careerType + positionCategoryId + techStacks 4중 복합 필터
 * → filter context: must + term + terms 복합
 * → 결과 건수가 제한적 → hit 수 적어도 filter 평가 비용은 동일
 */
function runMultiFilter() {
  const keyword      = pick([...HOT_KEYWORDS, ...GENERAL_KEYWORDS]);
  const careerType   = pick(['NEW', 'EXPERIENCED']);
  const catId        = pick(POSITION_CATEGORY_IDS);
  const techCombo    = pick(TECH_STACK_COMBOS);
  const sortBy       = pick(SORT_OPTIONS);

  const qs = buildQS({
    keyword,
    careerType,
    positionCategoryId: catId,
    techStacks: techCombo,
    sortBy,
    size: 20,
  });
  const url = `${BASE_URL}/api/job-summary/search?${qs}`;

  group('MULTI_FILTER', () => {
    doGet(url, {}, mMultiAnon);
    if (ACCESS_TOKEN) doGet(url, AUTH_HEADERS, mMultiAuth);
  });
}

/**
 * GROUP 3: LARGE_RESULT
 * 키워드 없음 + size=100
 * → 전체 문서 score 계산 불필요 (match_all) → OpenSearch fetch phase 비용
 * → 응답 payload 크기 최대 → 직렬화 + 네트워크 전송 + DB enrichment(로그인 시 100건 IN-query) 병목
 */
function runLargeResult() {
  const careerType = Math.random() < 0.5 ? 'EXPERIENCED' : null;
  const qs = buildQS({ careerType, sortBy: 'CREATED_AT_DESC', size: 100 });
  const url = `${BASE_URL}/api/job-summary/search?${qs}`;

  group('LARGE_RESULT', () => {
    doGet(url, {}, mLargeAnon);
    // 로그인 시 100건 IN-query → DB enrichment 최대 비용
    if (ACCESS_TOKEN) doGet(url, AUTH_HEADERS, mLargeAuth);
  });
}

/**
 * GROUP 4: CURSOR_PAGE
 * cursor 기반 연속 페이지네이션 (Search After)
 * → 1페이지 요청 → cursor 추출 → 2페이지 요청
 * → deep page일수록 OpenSearch sort value 비교 비용 증가
 * → cursor 없이 offset 방식과 달리 건너뛰는 비용이 없지만 sort field 정렬은 유지해야 함
 */
function runCursorPage() {
  const keyword = Math.random() < 0.6 ? pick([...HOT_KEYWORDS, ...GENERAL_KEYWORDS]) : null;
  const sortBy  = pick(['CREATED_AT_DESC', 'CREATED_AT_ASC']);
  const qs1     = buildQS({ keyword, sortBy, size: 20 });
  const url1    = `${BASE_URL}/api/job-summary/search?${qs1}`;

  group('CURSOR_PAGE', () => {
    // 1페이지
    const res1 = doGet(url1, {}, mCursorAnon);

    // cursor 추출 후 2페이지
    const cursor = extractCursor(res1);
    if (cursor) {
      const qs2  = buildQS({ keyword, sortBy, cursor, size: 20 });
      const url2 = `${BASE_URL}/api/job-summary/search?${qs2}`;
      doGet(url2, {}, mCursorAnon);
      if (ACCESS_TOKEN) {
        doGet(url1, AUTH_HEADERS, mCursorAuth);
        doGet(url2, AUTH_HEADERS, mCursorAuth);
      }
    } else {
      cursorMiss.add(1);
      // cursor 없으면 1페이지만 auth로 재측정
      if (ACCESS_TOKEN) doGet(url1, AUTH_HEADERS, mCursorAuth);
    }
  });
}

/**
 * GROUP 5: BRAND_FILTER
 * brandId + positionCategoryId ID 필터 (term filter, keyword 없음)
 * → scoring 계산 없음 → filter context만 → 가장 빠를 것으로 예상
 * → A/B 비교: 이 그룹이 느리면 ID 필터 인덱스 문제
 */
function runBrandFilter() {
  const brandId   = pick(BRAND_IDS);
  const catId     = pick(POSITION_CATEGORY_IDS);
  const careerType = Math.random() < 0.7 ? 'EXPERIENCED' : 'NEW';
  const sortBy    = pick(['CREATED_AT_DESC', 'CREATED_AT_ASC']);

  const qs = buildQS({ brandId, positionCategoryId: catId, careerType, sortBy, size: 20 });
  const url = `${BASE_URL}/api/job-summary/search?${qs}`;

  group('BRAND_FILTER', () => {
    doGet(url, {}, mBrandAnon);
    if (ACCESS_TOKEN) doGet(url, AUTH_HEADERS, mBrandAuth);
  });
}

/**
 * GROUP 6: TECH_STACK
 * techStacks 정확 매칭 (keyword field term 쿼리)
 * → seed에서 techStackParsed: keyword 타입으로 인덱싱
 * → 정확한 값 매칭 → 결과 건수 제한적
 * → keyword 없는 경우와 있는 경우를 혼합
 */
function runTechStack() {
  const techCombo = pick(TECH_STACK_COMBOS);
  const keyword   = Math.random() < 0.4 ? pick(HOT_KEYWORDS) : null;
  const sortBy    = keyword ? pick(['RELEVANCE', 'CREATED_AT_DESC']) : 'CREATED_AT_DESC';

  const qs = buildQS({ keyword, techStacks: techCombo, sortBy, size: 20 });
  const url = `${BASE_URL}/api/job-summary/search?${qs}`;

  group('TECH_STACK', () => {
    doGet(url, {}, mTechAnon);
    if (ACCESS_TOKEN) doGet(url, AUTH_HEADERS, mTechAuth);
  });
}

// ── default: 6개 그룹을 가중치 분포로 실행 ─────────────────────────────────
// 가중치 설계:
//   HOT_RELEVANCE  30% — hot keyword 집중 검색 재현
//   MULTI_FILTER   20% — 필터 사용자 패턴
//   LARGE_RESULT   10% — 전체 브라우징 패턴
//   CURSOR_PAGE    15% — 페이지 스크롤 패턴
//   BRAND_FILTER   15% — 회사/직군 필터 패턴
//   TECH_STACK     10% — 기술 스택 검색 패턴
export default function () {
  const r = Math.random();

  if      (r < 0.30) runHotRelevance();
  else if (r < 0.50) runMultiFilter();
  else if (r < 0.60) runLargeResult();
  else if (r < 0.75) runCursorPage();
  else if (r < 0.90) runBrandFilter();
  else               runTechStack();

  sleep(Math.random() * 0.5 + 0.1); // 0.1~0.6s
}

// ── handleSummary ────────────────────────────────────────────────────────────
export function handleSummary(data) {
  const m = data.metrics;

  function fmt(metric, pct) {
    return metric?.values?.[`p(${pct})`]?.toFixed(0) ?? 'N/A';
  }

  function diffRow(anonMetric, authMetric) {
    const a50 = anonMetric?.values?.['p(50)'];
    const au50 = authMetric?.values?.['p(50)'];
    const a99 = anonMetric?.values?.['p(99)'];
    const au99 = authMetric?.values?.['p(99)'];
    const d50 = (a50 != null && au50 != null) ? `+${(au50 - a50).toFixed(0)}` : 'N/A';
    const d99 = (a99 != null && au99 != null) ? `+${(au99 - a99).toFixed(0)}` : 'N/A';
    return `DB enrichment 비용  p50 ${d50}ms / p99 ${d99}ms`;
  }

  const errorPct = ((m.error_rate?.values?.rate ?? 0) * 100).toFixed(2);
  const cursorFail = m.cursor_extract_fail?.values?.count ?? 0;

  return {
    './results/A_search_load_complex_summary.json': JSON.stringify(data, null, 2),
    stdout: `
==============================================================
  시나리오 A-complex: OpenSearch 복합 쿼리 부하 테스트
==============================================================

  [GROUP 1] HOT_RELEVANCE  (hot keyword + RELEVANCE sort)
  비로그인  p50=${fmt(m.latency_hot_relevance_anon_ms,'50')}ms  p95=${fmt(m.latency_hot_relevance_anon_ms,'95')}ms  p99=${fmt(m.latency_hot_relevance_anon_ms,'99')}ms
  로그인    p50=${fmt(m.latency_hot_relevance_auth_ms,'50')}ms  p95=${fmt(m.latency_hot_relevance_auth_ms,'95')}ms  p99=${fmt(m.latency_hot_relevance_auth_ms,'99')}ms
  ${diffRow(m.latency_hot_relevance_anon_ms, m.latency_hot_relevance_auth_ms)}

  [GROUP 2] MULTI_FILTER   (keyword + careerType + positionCategoryId + techStacks)
  비로그인  p50=${fmt(m.latency_multi_filter_anon_ms,'50')}ms  p95=${fmt(m.latency_multi_filter_anon_ms,'95')}ms  p99=${fmt(m.latency_multi_filter_anon_ms,'99')}ms
  로그인    p50=${fmt(m.latency_multi_filter_auth_ms,'50')}ms  p95=${fmt(m.latency_multi_filter_auth_ms,'95')}ms  p99=${fmt(m.latency_multi_filter_auth_ms,'99')}ms
  ${diffRow(m.latency_multi_filter_anon_ms, m.latency_multi_filter_auth_ms)}

  [GROUP 3] LARGE_RESULT   (전체 조회 size=100)
  비로그인  p50=${fmt(m.latency_large_result_anon_ms,'50')}ms  p95=${fmt(m.latency_large_result_anon_ms,'95')}ms  p99=${fmt(m.latency_large_result_anon_ms,'99')}ms
  로그인    p50=${fmt(m.latency_large_result_auth_ms,'50')}ms  p95=${fmt(m.latency_large_result_auth_ms,'95')}ms  p99=${fmt(m.latency_large_result_auth_ms,'99')}ms
  ${diffRow(m.latency_large_result_anon_ms, m.latency_large_result_auth_ms)}

  [GROUP 4] CURSOR_PAGE    (Search After 연속 2페이지)
  비로그인  p50=${fmt(m.latency_cursor_page_anon_ms,'50')}ms  p95=${fmt(m.latency_cursor_page_anon_ms,'95')}ms  p99=${fmt(m.latency_cursor_page_anon_ms,'99')}ms
  로그인    p50=${fmt(m.latency_cursor_page_auth_ms,'50')}ms  p95=${fmt(m.latency_cursor_page_auth_ms,'95')}ms  p99=${fmt(m.latency_cursor_page_auth_ms,'99')}ms
  ${diffRow(m.latency_cursor_page_anon_ms, m.latency_cursor_page_auth_ms)}
  cursor 파싱 실패: ${cursorFail}회

  [GROUP 5] BRAND_FILTER   (brandId + positionCategoryId term filter)
  비로그인  p50=${fmt(m.latency_brand_filter_anon_ms,'50')}ms  p95=${fmt(m.latency_brand_filter_anon_ms,'95')}ms  p99=${fmt(m.latency_brand_filter_anon_ms,'99')}ms
  로그인    p50=${fmt(m.latency_brand_filter_auth_ms,'50')}ms  p95=${fmt(m.latency_brand_filter_auth_ms,'95')}ms  p99=${fmt(m.latency_brand_filter_auth_ms,'99')}ms
  ${diffRow(m.latency_brand_filter_anon_ms, m.latency_brand_filter_auth_ms)}

  [GROUP 6] TECH_STACK     (techStacks 정확 매칭)
  비로그인  p50=${fmt(m.latency_tech_stack_anon_ms,'50')}ms  p95=${fmt(m.latency_tech_stack_anon_ms,'95')}ms  p99=${fmt(m.latency_tech_stack_anon_ms,'99')}ms
  로그인    p50=${fmt(m.latency_tech_stack_auth_ms,'50')}ms  p95=${fmt(m.latency_tech_stack_auth_ms,'95')}ms  p99=${fmt(m.latency_tech_stack_auth_ms,'99')}ms
  ${diffRow(m.latency_tech_stack_anon_ms, m.latency_tech_stack_auth_ms)}

--------------------------------------------------------------
  Error Rate: ${errorPct}%
==============================================================
  [판독 포인트]
  HOT_RELEVANCE p99가 가장 높다면 → RELEVANCE sort scoring 비용 → query cache 검토
  LARGE_RESULT  p99가 튄다면     → size=100 fetch/직렬화 또는 DB IN-query(100건) 병목
  CURSOR_PAGE   p99가 높다면     → Search After sort value 비교 비용 → shard 수 조정
  BRAND_FILTER  p99가 높다면     → term filter 자체 문제 → 인덱스 설계 검토
  로그인 비용이 그룹마다 다르다면 → IN-query 크기(result size)에 비례 → DB enrichment 최적화
==============================================================
`,
  };
}
