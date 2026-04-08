#!/usr/bin/env bash
# HireLog 부하 테스트 시드 실행 스크립트 (인터랙티브)
#
# 사용법:
#   cd .claude/test/seed
#   chmod +x run_seed.sh
#   ./run_seed.sh

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SEED_SCRIPT="$SCRIPT_DIR/seed_opensearch.py"
VENV_DIR="$SCRIPT_DIR/.venv"

# ── 접속 설정 (환경변수로 오버라이드 가능) ──────────────────────────────────
OS_HOST="${OS_HOST:-localhost}"
OS_PORT="${OS_PORT:-9200}"
OS_USER="${OS_USER:-admin}"
OS_PASSWORD="${OS_PASSWORD:-admin}"
OS_INDEX="${OS_INDEX:-job_summary}"

PG_HOST="${PG_HOST:-localhost}"
PG_PORT="${PG_PORT:-5432}"
PG_DB="${PG_DB:-hirelog_dev}"
PG_USER="${PG_USER:-hirelog}"
PG_PASSWORD="${PG_PASSWORD:-1234}"
PG_DSN="host=${PG_HOST} port=${PG_PORT} dbname=${PG_DB} user=${PG_USER} password=${PG_PASSWORD}"

MEMBER_COUNT="${MEMBER_COUNT:-1000}"
MEMBER_JOB_SUMMARY_COUNT="${MEMBER_JOB_SUMMARY_COUNT:-80000}"

# ── 색상 ────────────────────────────────────────────────────────────────────
BOLD='\033[1m'
CYAN='\033[0;36m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
RED='\033[0;31m'
RESET='\033[0m'

# ── 유틸 ────────────────────────────────────────────────────────────────────
println()    { echo -e "$1"; }
info()       { echo -e "  ${CYAN}→${RESET} $1"; }
success()    { echo -e "  ${GREEN}✔${RESET} $1"; }
warn()       { echo -e "  ${YELLOW}!${RESET} $1"; }
error_exit() { echo -e "  ${RED}✖${RESET} $1"; exit 1; }
divider()    { echo -e "${CYAN}────────────────────────────────────────${RESET}"; }

# ── 가상환경 설정 ────────────────────────────────────────────────────────────
check_python() {
  command -v python3 &>/dev/null || error_exit "python3가 설치되어 있지 않습니다."
}

setup_venv() {
  if [ ! -d "$VENV_DIR" ]; then
    info "가상환경 생성 중: $VENV_DIR"
    python3 -m venv "$VENV_DIR"
  fi

  if [ -f "$VENV_DIR/Scripts/activate" ]; then
    source "$VENV_DIR/Scripts/activate"   # Windows Git Bash
  else
    source "$VENV_DIR/bin/activate"       # macOS / Linux
  fi

  pip install --quiet opensearch-py psycopg2-binary
}

# ── OpenSearch 연결 확인 ────────────────────────────────────────────────────
check_opensearch() {
  info "OpenSearch 연결 확인 중 ($OS_HOST:$OS_PORT)..."
  curl -sf -u "$OS_USER:$OS_PASSWORD" \
    "http://$OS_HOST:$OS_PORT/_cluster/health" > /dev/null \
    || error_exit "OpenSearch에 연결할 수 없습니다. ($OS_HOST:$OS_PORT)"
  success "OpenSearch 연결 OK"
}

# ── 문서 수 확인 ────────────────────────────────────────────────────────────
check_count() {
  local count
  count=$(curl -sf -u "$OS_USER:$OS_PASSWORD" \
    "http://$OS_HOST:$OS_PORT/$OS_INDEX/_count" \
    | python3 -c "import sys,json; print(json.load(sys.stdin)['count'])" 2>/dev/null || echo "조회 실패")
  success "OpenSearch [${OS_INDEX}] 현재 문서 수: ${BOLD}${count}${RESET}"
}

# ── 실행 함수 ───────────────────────────────────────────────────────────────
run_seed() {
  python3 "$SEED_SCRIPT" "$@"
}

# JOB_COUNT 확정 후 호출
build_args() {
  OS_ARGS=(
    --os-host "$OS_HOST"
    --os-port "$OS_PORT"
    --os-user "$OS_USER"
    --os-password "$OS_PASSWORD"
    --os-index "$OS_INDEX"
    --job-count "$JOB_COUNT"
  )
  DB_ARGS=(
    --pg-dsn "$PG_DSN"
    --member-count "$MEMBER_COUNT"
    --member-job-summary-count "$MEMBER_JOB_SUMMARY_COUNT"
  )
}

# ── 1단계: 적재 대상 선택 ────────────────────────────────────────────────────
select_target() {
  println ""
  divider
  println " ${BOLD}[1단계] 적재 대상 선택${RESET}"
  divider
  println "  1) OpenSearch만"
  println "  2) PostgreSQL만"
  println "  3) OpenSearch + PostgreSQL 전체"
  println "  0) 종료"
  divider
  printf "  선택 [0-3]: "
  read -r TARGET_INPUT

  case "$TARGET_INPUT" in
    1) SEED_TARGET="os"  ;;
    2) SEED_TARGET="db"  ;;
    3) SEED_TARGET="all" ;;
    0) println "종료합니다."; exit 0 ;;
    *) warn "잘못된 입력입니다. 다시 실행해 주세요."; exit 1 ;;
  esac
}

# ── 2단계: 초기화 옵션 선택 ──────────────────────────────────────────────────
select_reset() {
  println ""
  divider
  println " ${BOLD}[2단계] 초기화 옵션 선택${RESET}"
  divider

  RESET_OS=false
  RESET_DB=false

  case "$SEED_TARGET" in
    os)
      println "  1) 초기화 없이 추가 적재"
      println "  2) OpenSearch 인덱스 초기화 후 재적재"
      divider
      printf "  선택 [1-2]: "
      read -r RESET_INPUT
      case "$RESET_INPUT" in
        1) ;;
        2) RESET_OS=true ;;
        *) warn "잘못된 입력입니다. 다시 실행해 주세요."; exit 1 ;;
      esac
      ;;
    db)
      println "  1) 초기화 없이 추가 적재"
      println "  2) DB 테이블 초기화 후 재적재"
      divider
      printf "  선택 [1-2]: "
      read -r RESET_INPUT
      case "$RESET_INPUT" in
        1) ;;
        2) RESET_DB=true ;;
        *) warn "잘못된 입력입니다. 다시 실행해 주세요."; exit 1 ;;
      esac
      ;;
    all)
      println "  1) 초기화 없이 추가 적재"
      println "  2) OpenSearch 인덱스만 초기화"
      println "  3) DB 테이블만 초기화"
      println "  4) 전체 초기화 (OS + DB)"
      divider
      printf "  선택 [1-4]: "
      read -r RESET_INPUT
      case "$RESET_INPUT" in
        1) ;;
        2) RESET_OS=true ;;
        3) RESET_DB=true ;;
        4) RESET_OS=true; RESET_DB=true ;;
        *) warn "잘못된 입력입니다. 다시 실행해 주세요."; exit 1 ;;
      esac
      ;;
  esac
}

# ── 3단계: 삽입 수 선택 ──────────────────────────────────────────────────────
select_job_count() {
  local hint=""
  [[ "$SEED_TARGET" == "db" ]] && hint=" ${YELLOW}(job_id 범위 기준, OS 미사용)${RESET}"

  println ""
  divider
  println " ${BOLD}[3단계] 삽입할 문서 수 선택${RESET}${hint}"
  divider
  println "  1) 1만   (10,000)    — 빠른 연결 테스트"
  println "  2) 10만  (100,000)   — 기능 검증"
  println "  3) 50만  (500,000)   — 준 부하"
  println "  4) 100만 (1,000,000) — 풀 부하 (Hot keyword 집중 재현)"
  println "  5) 직접 입력"
  divider
  printf "  선택 [1-5]: "
  read -r COUNT_INPUT

  case "$COUNT_INPUT" in
    1) JOB_COUNT=10000   ;;
    2) JOB_COUNT=100000  ;;
    3) JOB_COUNT=500000  ;;
    4) JOB_COUNT=1000000 ;;
    5)
      printf "  삽입할 문서 수를 입력하세요: "
      read -r CUSTOM_COUNT
      [[ "$CUSTOM_COUNT" =~ ^[1-9][0-9]*$ ]] \
        || error_exit "올바른 숫자를 입력해야 합니다."
      JOB_COUNT="$CUSTOM_COUNT"
      ;;
    *) warn "잘못된 입력입니다. 다시 실행해 주세요."; exit 1 ;;
  esac
}

# ── 실행 전 요약 확인 ────────────────────────────────────────────────────────
confirm_summary() {
  println ""
  divider
  println " ${BOLD}[실행 요약]${RESET}"
  divider

  case "$SEED_TARGET" in
    os)  println "  대상     : OpenSearch" ;;
    db)  println "  대상     : PostgreSQL" ;;
    all) println "  대상     : OpenSearch + PostgreSQL" ;;
  esac

  local reset_label="없음"
  if [[ "$RESET_OS" == true && "$RESET_DB" == true ]]; then
    reset_label="OS 인덱스 + DB 테이블 초기화"
  elif [[ "$RESET_OS" == true ]]; then
    reset_label="OpenSearch 인덱스 초기화"
  elif [[ "$RESET_DB" == true ]]; then
    reset_label="DB 테이블 초기화"
  fi
  println "  초기화   : $reset_label"
  println "  문서 수  : ${BOLD}$(printf '%\047d' "$JOB_COUNT")건${RESET}"

  if [[ "$SEED_TARGET" == "db" || "$SEED_TARGET" == "all" ]]; then
    println "  회원 수  : $MEMBER_COUNT명"
    println "  저장 수  : $MEMBER_JOB_SUMMARY_COUNT건"
  fi

  if [[ "$SEED_TARGET" != "db" ]]; then
    println "  OS       : $OS_HOST:$OS_PORT / index=$OS_INDEX"
  fi
  if [[ "$SEED_TARGET" == "db" || "$SEED_TARGET" == "all" ]]; then
    println "  DB       : $PG_HOST:$PG_PORT / $PG_DB"
  fi

  divider
  printf "  진행하시겠습니까? [y/N]: "
  read -r CONFIRM
  [[ "$CONFIRM" =~ ^[Yy]$ ]] || { println "취소되었습니다."; exit 0; }
}

# ── main ────────────────────────────────────────────────────────────────────
println ""
println " ${BOLD}${CYAN}HireLog 시드 데이터 삽입${RESET}"

check_python
setup_venv

select_target
select_reset
select_job_count
confirm_summary
build_args   # JOB_COUNT 확정 후 배열 구성

println ""
info "시드 시작..."
println ""

# reset 플래그 조립
RESET_FLAGS=()
[[ "$RESET_OS" == true ]] && RESET_FLAGS+=(--reset-os)
[[ "$RESET_DB" == true ]] && RESET_FLAGS+=(--reset-db)

case "$SEED_TARGET" in
  os)
    check_opensearch
    run_seed --seed-os "${RESET_FLAGS[@]}" "${OS_ARGS[@]}"
    ;;
  db)
    run_seed --seed-db "${RESET_FLAGS[@]}" "${DB_ARGS[@]}"
    ;;
  all)
    check_opensearch
    run_seed --seed-os --seed-db "${RESET_FLAGS[@]}" "${OS_ARGS[@]}" "${DB_ARGS[@]}"
    ;;
esac

println ""
divider

if [[ "$SEED_TARGET" != "db" ]]; then
  check_count
fi

success "완료"
divider