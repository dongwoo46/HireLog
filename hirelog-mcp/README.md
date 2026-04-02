# hirelog-mcp

HireLog용 MCP 서버입니다. 로컬 클라이언트(`stdio`)와 웹 클라이언트(`remote HTTP`)를 모두 지원합니다.

## 연결 대상

- Claude Desktop: 로컬 `stdio`
- Codex(CLI/데스크톱 설정): 로컬 `stdio`
- ChatGPT Web: 원격 MCP URL(공개 HTTPS)
- Claude Web: 원격 MCP URL(공개 HTTPS)

중요: ChatGPT Web/Claude Web은 로컬 `stdio` 프로세스에 직접 연결할 수 없습니다.  
웹에서는 반드시 `start:http`로 실행한 MCP 서버를 HTTPS로 노출해 연결해야 합니다.

## 제공 도구

- `ping`: 서버 생존 확인
- `hirelog_health`: `GET /actuator/health`
- `search_jd`: 키워드 기반 JD 검색 (`GET /api/job-summary/search`)
- `jd_register`: URL 기반 JD 등록 (`POST /api/job-summary/url`)
- `jd_register_text`: 텍스트 기반 JD 등록 (`POST /api/job-summary/text`)
- `jd_get_detail`: JD 상세 조회 (`GET /api/job-summary/{id}`)
- `jd_list`: JD 목록/검색 (`GET /api/job-summary/search`)
- `my_applied_jd`: 내가 지원한 JD 목록 (`saveType=APPLY`)
- `my_saved_jd`: 내가 저장한 JD 목록 (`saveType=SAVED`)

## 설치 및 빌드

```bash
cd hirelog-mcp
npm.cmd install
npm.cmd run build
```

## 실행

로컬 `stdio` 모드:

```bash
cd hirelog-mcp
npm.cmd run start
```

원격 HTTP 모드:

```bash
cd hirelog-mcp
npm.cmd run start:http
```

기본 HTTP 엔드포인트:

- `http://0.0.0.0:8787/mcp`

## 환경 변수

공통:

- `HIRELOG_API_BASE_URL` (기본값: `https://hirelog.kro.kr`)
- `VITE_API_BASE_URL` (`HIRELOG_API_BASE_URL` 미설정 시 fallback)
- `HIRELOG_API_BEARER_TOKEN` (선택, Bearer 인증 토큰)
- `HIRELOG_API_COOKIE` (선택, Cookie 기반 인증 필요 시 직접 전달)

HTTP 모드:

- `PORT` (기본값: `8787`)
- `HOST` (기본값: `0.0.0.0`)
- `MCP_PATH` (기본값: `/mcp`)
- `MCP_AUTH_TOKEN` (선택, 원격 운영 시 권장)

`MCP_AUTH_TOKEN`을 설정한 경우 요청 헤더:

- `Authorization: Bearer <MCP_AUTH_TOKEN>`

## Claude Desktop 설정 (로컬 stdio)

1. Windows 설정 파일 열기  
`%APPDATA%\\Claude\\claude_desktop_config.json`
2. 예시 파일 내용 반영  
`hirelog-mcp/examples/claude_desktop_config.json`
3. Claude Desktop 재시작

## Codex 설정 (로컬 stdio)

1. Codex 설정 파일 열기  
`~/.codex/config.toml` (Windows: `C:\\Users\\<you>\\.codex\\config.toml`)
2. 예시 파일 내용 반영  
`hirelog-mcp/examples/codex-config.toml`
3. Codex 세션 재시작

## ChatGPT Web / Claude Web 설정 (원격 MCP)

1. `npm.cmd run start:http` 실행
2. `http://localhost:8787/mcp`를 공개 HTTPS URL로 노출
3. 웹 설정에서 해당 HTTPS MCP URL 등록
4. `MCP_AUTH_TOKEN` 사용 시 Bearer 인증도 함께 등록

## Linux + GitHub Actions 배포

이 저장소에는 MCP 배포 워크플로우가 추가되어 있습니다.

- 워크플로우 파일: `.github/workflows/deploy-mcp.yml`
- 동작: `hirelog-mcp/**`가 `develop -> main`으로 머지되면 자동 배포
- 방식: 빌드 후 서버 업로드 + `systemd` 서비스(`hirelog-mcp`) 재시작

필수 GitHub Secrets:

- `SERVER_API_HOST`
- `SERVER_USER`
- `SERVER_SSH_KEY`
- `MCP_ENV_PROD`

`MCP_ENV_PROD` 예시:

```env
HIRELOG_API_BASE_URL=https://hirelog.kro.kr
MCP_AUTH_TOKEN=change-me
PORT=8787
HOST=0.0.0.0
MCP_PATH=/mcp
```

## 빠른 확인

```bash
curl http://localhost:8787/
```

정상 응답:

- `hirelog-mcp http server is running`
