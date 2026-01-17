#!/usr/bin/env bash
set -e

IMAGE=hirelog-ocr-dev

# 스크립트 위치 기준으로 프로젝트 루트 계산 (bash 기준)
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Git Bash 경로 → Windows 절대경로로 변환 (build 용)
PIPELINE_DIR_WIN="$(cygpath -w "$PROJECT_ROOT/text-pipeline")"

echo "[1/2] Docker image build"
docker build -t $IMAGE "$PIPELINE_DIR_WIN"

echo "[2/2] Run OCR container"
MSYS_NO_PATHCONV=1 docker run --rm \
  -v "$PROJECT_ROOT/text-pipeline:/app" \
  -v "$PROJECT_ROOT/text-pipeline/data:/app/data" \
  $IMAGE
