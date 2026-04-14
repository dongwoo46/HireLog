#!/usr/bin/env bash
# OpenSearch 인덱스 초기화 전용 스크립트 (QA 환경 리셋용)
#
# 사용법:
#   cd .claude/test/seed
#   chmod +x reset_opensearch.sh
#   ./reset_opensearch.sh
#
# 주의: 인덱스 삭제만 수행. 재생성은 Spring Boot Admin API 사용.
#   POST /api/admin/job-summary/reindex-all

set -euo pipefail

OS_HOST="${OS_HOST:-localhost}"
OS_PORT="${OS_PORT:-9200}"
OS_USER="${OS_USER:-admin}"
OS_PASSWORD="${OS_PASSWORD:-admin}"
OS_INDEX="${OS_INDEX:-job_summary}"

BASE_URL="http://$OS_HOST:$OS_PORT"
AUTH="-u $OS_USER:$OS_PASSWORD"

BOLD='\033[1m'
CYAN='\033[0;36m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
RED='\033[0;31m'
RESET='\033[0m'

divider() { echo -e "${CYAN}────────────────────────────────────────${RESET}"; }
info()    { echo -e "  ${CYAN}→${RESET} $1"; }
success() { echo -e "  ${GREEN}✔${RESET} $1"; }
warn()    { echo -e "  ${YELLOW}!${RESET} $1"; }
die()     { echo -e "  ${RED}✖${RESET} $1"; exit 1; }

# ── 연결 확인 ──────────────────────────────────────────────────────────────────
check_connection() {
  info "OpenSearch 연결 확인 중 ($OS_HOST:$OS_PORT)..."
  curl -sf $AUTH "$BASE_URL/_cluster/health" > /dev/null \
    || die "OpenSearch에 연결할 수 없습니다. ($BASE_URL)"
  success "연결 OK"
}

# ── 인덱스 존재 확인 ───────────────────────────────────────────────────────────
check_index_exists() {
  HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" $AUTH "$BASE_URL/$OS_INDEX")
  echo "$HTTP_STATUS"
}

# ── 문서 수 조회 ───────────────────────────────────────────────────────────────
get_doc_count() {
  curl -sf $AUTH "$BASE_URL/$OS_INDEX/_count" \
    | python3 -c "import sys,json; print(json.load(sys.stdin).get('count','조회 실패'))" 2>/dev/null \
    || echo "조회 실패"
}

# ── main ───────────────────────────────────────────────────────────────────────
echo ""
echo -e " ${BOLD}${CYAN}HireLog OpenSearch 인덱스 초기화${RESET}"
divider

check_connection

STATUS=$(check_index_exists)

if [ "$STATUS" == "404" ]; then
  warn "인덱스 [$OS_INDEX] 가 존재하지 않습니다. 초기화 불필요."
  echo ""
  exit 0
fi

DOC_COUNT=$(get_doc_count)
info "현재 인덱스 [$OS_INDEX] 문서 수: ${BOLD}${DOC_COUNT}${RESET}건"
echo ""
divider

echo -e "  대상  : ${BOLD}$BASE_URL / $OS_INDEX${RESET}"
echo -e "  작업  : 인덱스 삭제 (매핑 포함)"
echo -e "  ${YELLOW}재생성은 Spring Boot Admin API 로 수행하세요:${RESET}"
echo -e "  ${YELLOW}  POST /api/admin/job-summary/reindex-all${RESET}"
divider
printf "  진행하시겠습니까? [y/N]: "
read -r CONFIRM
[[ "$CONFIRM" =~ ^[Yy]$ ]] || { echo "취소되었습니다."; exit 0; }

echo ""
info "인덱스 삭제 중..."

RESPONSE=$(curl -sf -X DELETE $AUTH "$BASE_URL/$OS_INDEX" 2>&1) || die "삭제 실패: $RESPONSE"

success "인덱스 [$OS_INDEX] 삭제 완료"
divider
echo ""
echo -e "  ${YELLOW}다음 단계:${RESET}"
echo -e "  1) Spring Boot 서버 기동 확인"
echo -e "  2) POST /api/admin/job-summary/reindex-all?batchSize=50"
echo ""