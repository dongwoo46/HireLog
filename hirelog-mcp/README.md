# hirelog-mcp

HireLog MCP 서버입니다. 로컬(`stdio`)과 원격 HTTP(`remote MCP`)를 지원합니다.

## 운영 모드 분리

이 서버는 환경변수로 공개/비공개 모드를 분리할 수 있습니다.

- `MCP_PUBLIC_READONLY=true`  
  공개 읽기 전용 모드. 조회 도구만 노출됩니다.
- `MCP_PUBLIC_READONLY=false`  
  비공개 전체 모드. 등록/개인 목록 도구까지 포함됩니다.

권장 운영:

1. 공개용 MCP: `MCP_PUBLIC_READONLY=true` + 별도 도메인/엔드포인트
2. 비공개 MCP: `MCP_PUBLIC_READONLY=false` + 토큰 필수

## 혼합 모드 (권장)

아래처럼 설정하면 하나의 서버에서 둘 다 가능합니다.

- 토큰 없이 호출: 읽기 도구만 사용
- 올바른 `MCP_AUTH_TOKEN`으로 호출: 읽기 + 쓰기/개인 도구 사용

설정:

```env
MCP_PUBLIC_READONLY=false
MCP_AUTH_TOKEN=your-secret-token
```

## 도구 목록

공통(읽기):

- `ping`
- `hirelog_health`
- `search_jd`
- `jd_get_detail`
- `jd_list`

비공개 전용(쓰기/개인):

- `jd_register`
- `jd_register_text`
- `my_applied_jd`
- `my_saved_jd`

## 환경 변수

공통:

- `HIRELOG_API_BASE_URL` (기본값: `https://hirelog.kro.kr`)
- `VITE_API_BASE_URL` (`HIRELOG_API_BASE_URL` 미설정 시 fallback)
- `HIRELOG_API_BEARER_TOKEN` (선택)
- `HIRELOG_API_COOKIE` (선택)
- `MCP_PUBLIC_READONLY` (기본값: `false`)

HTTP:

- `PORT` (기본값: `8787`)
- `HOST` (기본값: `0.0.0.0`)
- `MCP_PATH` (기본값: `/mcp`)
- `MCP_AUTH_TOKEN` (권장)
- `MCP_READ_RATE_LIMIT_PER_MIN` (기본값: `120`)
- `MCP_WRITE_RATE_LIMIT_PER_MIN` (기본값: `20`)

## 빠른 로컬 실행

```bash
cd hirelog-mcp
npm install
npm run build
npm run start:http
```

## Docker 배포 (GitHub Actions)

워크플로우:

- `.github/workflows/deploy-mcp.yml`

동작:

1. Docker 이미지 빌드
2. GHCR(`ghcr.io/dongwoo46/hirelog-mcp`) 푸시
3. 서버에서 이미지 pull
4. 컨테이너 재기동

필수 GitHub Secrets:

- `SERVER_API_HOST`
- `SERVER_USER`
- `SERVER_SSH_KEY`
- `MCP_ENV_PROD`

`MCP_ENV_PROD` 예시 (비공개 전체):

```env
HIRELOG_API_BASE_URL=https://hirelog.kro.kr
MCP_PUBLIC_READONLY=false
MCP_AUTH_TOKEN=your-random-long-secret
PORT=8787
HOST=0.0.0.0
MCP_PATH=/mcp
```

`MCP_ENV_PROD` 예시 (공개 읽기전용):

```env
HIRELOG_API_BASE_URL=https://hirelog.kro.kr
MCP_PUBLIC_READONLY=true
# 공개 서버는 토큰 없이 열 수도 있지만, 보안을 위해 토큰 권장
MCP_AUTH_TOKEN=optional-but-recommended
PORT=8787
HOST=0.0.0.0
MCP_PATH=/mcp
```

## Nginx 연결

- `https://.../mcp` -> `http://127.0.0.1:8787/mcp` 프록시
- `Authorization` 헤더 전달

## 상태 확인

```bash
curl http://127.0.0.1:8787/
```

정상 응답:

- `hirelog-mcp http server is running`
