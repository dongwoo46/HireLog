import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';

/**
 * ============================================================
 * SUSTAINED LOAD TEST (Long Stability Test)
 * ------------------------------------------------------------
 * 목적:
 * - 장시간 일정 부하에서 시스템 안정성 검증
 * - Kafka backlog 누적 여부 확인
 * - DB connection pool 안정성 확인
 * - OpenSearch 인덱싱 지연 누적 여부 확인
 *
 * Stress와 다르게:
 * - 한계 탐색이 아니라
 * - "안정 구간에서 15분 유지"가 목적
 * ============================================================
 */

// ---------- Custom Metrics ----------
export const latencyTrend = new Trend('http_latency_ms');
export const failureRate = new Rate('http_failure_rate');

// ---------- k6 Options ----------
export const options = {
  scenarios: {
    sustained: {
      executor: 'constant-vus',
      vus: 60, // baseline보다 약간 높은 수준
      duration: '15m',
    },
  },
  thresholds: {
    http_failure_rate: ['rate<0.02'],
    http_latency_ms: ['p(95)<2000'],
  },
};

// ---------- Target ----------
const BASE_URL = 'http://localhost:8080';
const ACCESS_TOKEN = 'YOUR_TOKEN';

// ---------- Setup ----------
export function setup() {
  console.log('===== SUSTAINED TEST START =====');
  console.log('▶ Kafka Lag 누적 여부 확인');
  console.log('▶ Debezium lag 증가 여부 확인');
  console.log('▶ Hikari Active / Pending 모니터링');
  console.log('▶ OpenSearch indexing rate 확인');
}

// ---------- Test Logic ----------
export default function () {
  const payload = JSON.stringify({
    brandName: 'SustainedBrand',
    brandPositionName: 'Backend Developer',
    jdText:
      `SUSTAINED_${__VU}_${__ITER}` +
      '[네이버웹툰] 웹툰 콘텐츠 관리 플랫폼 Back-End 개발 (경력)\n\n모집 조직: NAVER WEBTOON\n직무 분야: Backend\n고용 형태: 정규직\n접수 일정: 2025.12.30 ~ 2026.01.18 (23:59)\n\n조직 안내\n\n라인웹툰, 네이버웹툰, 라인망가 등 글로벌 사용자를 대상으로 하는 웹툰 서비스의 콘텐츠 운영 시스템을 개발합니다.\n\n국내외에서 활동하는 작가 및 협력사가 제작한 콘텐츠를 안정적으로 저장하고, 각 서비스에 빠르게 배포(Publish)할 수 있는 웹툰 전반의 콘텐츠 플랫폼을 설계하고 운영합니다.\n\n주요 업무\n\n회사의 핵심 IP 자산(이미지, 영상, 전자책 등)을 관리하는 플랫폼을 개발 및 개선합니다.\n작가의 작업 완료부터 실제 사용자 서비스 반영까지 이어지는 콘텐츠 처리 파이프라인 전반을 구현합니다.\n글로벌 사용자 환경을 고려한 서비스 기능을 개발합니다.\n수 KB에서 수 GB까지 다양한 크기의 대용량 콘텐츠 파일을 국내외 작가 및 협업사에게 신속하게 업로드/다운로드할 수 있는 시스템을 구축합니다.\n\n자격 요건\n\n- Kotlin 또는 Java 기반 Spring Framework 웹 서버 개발 경력 3년 이상\n- 웹 인프라 구성 요소 이해 (nginx, tomcat, netty, redis, spring-batch, kafka 등)\n- REST API 설계 및 구현 경험\n- RDBMS 및 NoSQL 데이터베이스 활용 경험\n- Git 기반 협업 경험\n\n우대 조건\n\n- gRPC 또는 GraphQL 활용 경험\n- 비동기 및 논블로킹 아키텍처 개발 경험\n- 클라우드 환경에서의 컨테이너 및 MSA 경험\n- React, Vue 등 최신 JavaScript 프레임워크 경험\n- LLM(OpenAI, Gemini 등)을 활용한 서비스 개발 경험\n- AI 개발 도구(Cursor, Claude Code 등) 활용 경험\n- 대규모 트래픽 대응 경험\n- 글로벌 서비스 운영 경험\n- 관심사 분리를 고려한 유연한 시스템 설계 역량\n- 새로운 기술에 대한 학습 의지\n- 웹툰/웹소설 산업에 대한 관심',
  });

  const res = http.post(`${BASE_URL}/api/job-summary/text`, payload, {
    headers: {
      'Content-Type': 'application/json',
      Cookie: `access_token=${ACCESS_TOKEN}`,
    },
  });

  const success = check(res, {
    'status 200': (r) => r.status === 200,
  });

  latencyTrend.add(res.timings.duration);
  failureRate.add(!success);

  /**
   * 현실적인 사용자 think time
   */
  sleep(0.2);
}

/**
 * ============================================================
 * Sustained 종료 후 반드시 확인
 * ============================================================
 *
 * 1) Kafka lag가 시간이 지날수록 증가하는가?
 * 2) Debezium lag 누적되는가?
 * 3) DB active connection 증가 추세 있는가?
 * 4) GC / 메모리 사용량 증가하는가?
 * 5) OpenSearch indexing delay 발생하는가?
 *
 * 정상 기준:
 * - Lag 0 유지 또는 안정화
 * - backlog 누적 없음
 * - 메모리/커넥션 안정
 *
 * ============================================================
 */
