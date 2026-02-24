import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend } from 'k6/metrics';

/**
 * ============================================================
 * STRESS TEST (Throughput Ceiling Discovery)
 * ------------------------------------------------------------
 * 목적:
 * - 시스템의 처리 한계 지점 탐색
 * - Kafka Lag 발생 시점 측정
 * - Producer / Consumer 처리량 차이 분석
 *
 * Baseline과 달리:
 * - 안정성 보장 목적이 아님
 * - 어디서 무너지는지 찾는 실험
 *
 * 분석 포인트:
 * - Kafka Lag 증가 시작 시점
 * - Debezium lag 변화
 * - Python msg/sec 처리량
 * - OpenSearch indexing rate
 * ============================================================
 */

// ---------- Custom Metrics ----------
export const latencyTrend = new Trend('http_latency_ms');

// ---------- k6 Options ----------
export const options = {
  scenarios: {
    stress: {
      executor: 'ramping-vus',
      startVUs: 40, // baseline 근처에서 시작
      stages: [
        { duration: '1m', target: 80 }, // 1단계
        { duration: '1m', target: 150 }, // 2단계
        { duration: '1m', target: 250 }, // 3단계
        { duration: '1m', target: 350 }, // 4단계 (한계 탐색)
        { duration: '30s', target: 0 },
      ],
      gracefulRampDown: '20s',
    },
  },
};

// ---------- Target ----------
const BASE_URL = 'http://localhost:8080';
const ACCESS_TOKEN = 'YOUR_TOKEN';

// ---------- Test Logic ----------
export default function () {
  const payload = JSON.stringify({
    brandName: 'TestBrand',
    brandPositionName: 'Backend Developer',
    jdText:
      `STRESS_${__VU}_${__ITER}` +
      '[네이버웹툰] 웹툰 콘텐츠 관리 플랫폼 Back-End 개발 (경력)\n\n모집 조직: NAVER WEBTOON\n직무 분야: Backend\n고용 형태: 정규직\n접수 일정: 2025.12.30 ~ 2026.01.18 (23:59)\n\n조직 안내\n\n라인웹툰, 네이버웹툰, 라인망가 등 글로벌 사용자를 대상으로 하는 웹툰 서비스의 콘텐츠 운영 시스템을 개발합니다.\n\n국내외에서 활동하는 작가 및 협력사가 제작한 콘텐츠를 안정적으로 저장하고, 각 서비스에 빠르게 배포(Publish)할 수 있는 웹툰 전반의 콘텐츠 플랫폼을 설계하고 운영합니다.\n\n주요 업무\n\n회사의 핵심 IP 자산(이미지, 영상, 전자책 등)을 관리하는 플랫폼을 개발 및 개선합니다.\n작가의 작업 완료부터 실제 사용자 서비스 반영까지 이어지는 콘텐츠 처리 파이프라인 전반을 구현합니다.\n글로벌 사용자 환경을 고려한 서비스 기능을 개발합니다.\n수 KB에서 수 GB까지 다양한 크기의 대용량 콘텐츠 파일을 국내외 작가 및 협업사에게 신속하게 업로드/다운로드할 수 있는 시스템을 구축합니다.\n\n자격 요건\n\n- Kotlin 또는 Java 기반 Spring Framework 웹 서버 개발 경력 3년 이상\n- 웹 인프라 구성 요소 이해 (nginx, tomcat, netty, redis, spring-batch, kafka 등)\n- REST API 설계 및 구현 경험\n- RDBMS 및 NoSQL 데이터베이스 활용 경험\n- Git 기반 협업 경험\n\n우대 조건\n\n- gRPC 또는 GraphQL 활용 경험\n- 비동기 및 논블로킹 아키텍처 개발 경험\n- 클라우드 환경에서의 컨테이너 및 MSA 경험\n- React, Vue 등 최신 JavaScript 프레임워크 경험\n- LLM(OpenAI, Gemini 등)을 활용한 서비스 개발 경험\n- AI 개발 도구(Cursor, Claude Code 등) 활용 경험\n- 대규모 트래픽 대응 경험\n- 글로벌 서비스 운영 경험\n- 관심사 분리를 고려한 유연한 시스템 설계 역량\n- 새로운 기술에 대한 학습 의지\n- 웹툰/웹소설 산업에 대한 관심',
  });

  const res = http.post(`${BASE_URL}/api/job-summary/text`, payload, {
    headers: {
      'Content-Type': 'application/json',
      Cookie: `access_token=${ACCESS_TOKEN}`,
    },
  });

  check(res, {
    'status 200': (r) => r.status === 200,
  });

  latencyTrend.add(res.timings.duration);

  /**
   * 짧은 think time
   * → baseline보다 강하게 밀어붙임
   */
  sleep(0.1);
}

/**
 * ============================================================
 * Stress 종료 후 반드시 확인
 * ============================================================
 *
 * 1) Kafka Consumer Lag peak 값
 * 2) Lag가 "계속 증가"하는지 or "안정화"되는지
 * 3) Python 처리량 (msg/sec)
 * 4) Producer rate vs Consumer rate 차이
 * 5) OpenSearch indexing rate
 *
 * 핵심 분석:
 *
 * 예시:
 * Producer: 220 msg/sec
 * Consumer: 180 msg/sec
 *
 * → backlog 40/sec 누적
 *
 * 60초면 2,400개 적체
 *
 * 이 수치로 병목을 증명해야 한다.
 *
 * ============================================================
 */
