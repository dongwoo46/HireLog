import http from 'k6/http';
import { check, sleep } from 'k6';

/**
 * ============================================================
 * CIRCUIT BREAKER TEST
 * ------------------------------------------------------------
 * 목적:
 * - LLM 연속 실패 발생 시 Circuit Breaker OPEN 확인
 * - 외부 API 폭주 차단 검증
 *
 * 전제:
 * - 잘못된 LLM API 키 또는 timeout 유도 상태
 * - Breaker threshold 설정 완료
 * ============================================================
 */

export const options = {
  scenarios: {
    breaker_test: {
      executor: 'constant-vus',
      vus: 150,
      duration: '5m',
    },
  },
};

const BASE_URL = 'http://localhost:8080';
const ACCESS_TOKEN = 'YOUR_TOKEN';

export function setup() {
  console.log('===== CIRCUIT BREAKER TEST START =====');
  console.log('▶ LLM 강제 실패 상태');
  console.log('▶ 일정 실패 후 breaker OPEN 여부 확인');
}

export default function () {
  const payload = JSON.stringify({
    brandName: 'BreakerBrand',
    brandPositionName: 'Backend',
    jdText:
      `BREAKER_TEST_${__VU}_${__ITER}` +
      '[네이버웹툰] 웹툰 콘텐츠 관리 플랫폼 Back-End 개발 (경력)\n\n모집 조직: NAVER WEBTOON\n직무 분야: Backend\n고용 형태: 정규직\n접수 일정: 2025.12.30 ~ 2026.01.18 (23:59)\n\n조직 안내\n\n라인웹툰, 네이버웹툰, 라인망가 등 글로벌 사용자를 대상으로 하는 웹툰 서비스의 콘텐츠 운영 시스템을 개발합니다.\n\n국내외에서 활동하는 작가 및 협력사가 제작한 콘텐츠를 안정적으로 저장하고, 각 서비스에 빠르게 배포(Publish)할 수 있는 웹툰 전반의 콘텐츠 플랫폼을 설계하고 운영합니다.\n\n주요 업무\n\n회사의 핵심 IP 자산(이미지, 영상, 전자책 등)을 관리하는 플랫폼을 개발 및 개선합니다.\n작가의 작업 완료부터 실제 사용자 서비스 반영까지 이어지는 콘텐츠 처리 파이프라인 전반을 구현합니다.\n글로벌 사용자 환경을 고려한 서비스 기능을 개발합니다.\n수 KB에서 수 GB까지 다양한 크기의 대용량 콘텐츠 파일을 국내외 작가 및 협업사에게 신속하게 업로드/다운로드할 수 있는 시스템을 구축합니다.\n\n자격 요건\n\n- Kotlin 또는 Java 기반 Spring Framework 웹 서버 개발 경력 3년 이상\n- 웹 인프라 구성 요소 이해 (nginx, tomcat, netty, redis, spring-batch, kafka 등)\n- REST API 설계 및 구현 경험\n- RDBMS 및 NoSQL 데이터베이스 활용 경험\n- Git 기반 협업 경험\n\n우대 조건\n\n- gRPC 또는 GraphQL 활용 경험\n- 비동기 및 논블로킹 아키텍처 개발 경험\n- 클라우드 환경에서의 컨테이너 및 MSA 경험\n- React, Vue 등 최신 JavaScript 프레임워크 경험\n- LLM(OpenAI, Gemini 등)을 활용한 서비스 개발 경험\n- AI 개발 도구(Cursor, Claude Code 등) 활용 경험\n- 대규모 트래픽 대응 경험\n- 글로벌 서비스 운영 경험\n- 관심사 분리를 고려한 유연한 시스템 설계 역량\n- 새로운 기술에 대한 학습 의지\n- 웹툰/웹소설 산업에 대한 관심',
  });

  const res = http.post(`${BASE_URL}/api/job-summary/text`, payload, {
    headers: {
      'Content-Type': 'application/json',
      Cookie: `access_token=${ACCESS_TOKEN}`,
    },
  });

  check(res, {
    'Request processed (200 or fallback)': (r) =>
      r.status === 200 || r.status >= 500,
  });

  sleep(0.1);
}
