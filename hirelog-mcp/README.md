# hirelog-mcp

HireLog용 MCP 서버입니다. 로컬(`stdio`)과 원격 HTTP(`remote MCP`)를 모두 지원합니다.

## 연결 대상

- Claude Desktop: 로컬 `stdio`
- Codex: 로컬 `stdio`
- ChatGPT Web: 원격 MCP URL(HTTPS)
- Claude Web: 원격 MCP URL(HTTPS)

## 제공 도구

- `ping`
- `hirelog_health`
- `search_jd`
- `jd_register` (URL 등록)
- `jd_register_text` (텍스트 등록)
- `jd_get_detail`
- `jd_list`
- `my_applied_jd`
- `my_saved_jd`

## 로컬 실행

```bash
cd hirelog-mcp
npm install
npm run build
npm run start:http
```

기본 엔드포인트:

- `http://0.0.0.0:8787/mcp`

## 환경 변수

공통:

- `HIRELOG_API_BASE_URL` (기본값: `https://hirelog.kro.kr`)
- `VITE_API_BASE_URL` (`HIRELOG_API_BASE_URL` 미설정 시 fallback)
- `HIRELOG_API_BEARER_TOKEN` (선택)
- `HIRELOG_API_COOKIE` (선택)

HTTP 모드:

- `PORT` (기본값: `8787`)
- `HOST` (기본값: `0.0.0.0`)
- `MCP_PATH` (기본값: `/mcp`)
- `MCP_AUTH_TOKEN` (권장)

`MCP_AUTH_TOKEN`을 설정했다면 클라이언트 요청 헤더:

- `Authorization: Bearer <MCP_AUTH_TOKEN>`

## Linux + GitHub Actions 배포 (Docker)

워크플로우:

- `.github/workflows/deploy-mcp.yml`

동작:

1. `hirelog-mcp` Docker 이미지 빌드
2. GHCR(`ghcr.io/dongwoo46/hirelog-mcp`)로 푸시
3. 서버에서 이미지 pull
4. 컨테이너 `hirelog-mcp` 재기동

필수 GitHub Secrets:

- `SERVER_API_HOST`
- `SERVER_USER`
- `SERVER_SSH_KEY`
- `MCP_ENV_PROD`

`MCP_ENV_PROD` 예시:

```env
HIRELOG_API_BASE_URL=https://hirelog.kro.kr
MCP_AUTH_TOKEN=your-random-long-secret
PORT=8787
HOST=0.0.0.0
MCP_PATH=/mcp
```

서버 요구사항:

- Docker 설치
- `docker` 명령 실행 가능한 권한

## Nginx 연동 예시

- `https://hirelog.kro.kr/mcp` -> `http://127.0.0.1:8787/mcp` 프록시
- `Authorization` 헤더 전달

## 빠른 점검

```bash
curl http://127.0.0.1:8787/
```

정상 응답:

- `hirelog-mcp http server is running`
