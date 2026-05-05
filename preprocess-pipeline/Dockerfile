FROM --platform=$TARGETPLATFORM python:3.12-slim

# PaddleOCR / PaddlePaddle C++ 내부 로그 억제
ENV GLOG_minloglevel=3 \
    FLAGS_minloglevel=3 \
    FLAGS_use_mkldnn=0 \
    DISABLE_MODEL_SOURCE_CHECK=True

# src/ 를 Python import 루트로 등록
ENV PYTHONPATH=/app/src \
    PYTHONUNBUFFERED=1 \
    PYTHONDONTWRITEBYTECODE=1

# Playwright 브라우저 저장 경로 고정 (비루트 사용자 전환 후에도 불변)
ENV PLAYWRIGHT_BROWSERS_PATH=/app/.playwright

# ─────────────────────────────────────────────────────────────────
# 시스템 의존성
#  - libgl1, libglib2.0-0  : OpenCV headless 필수
#  - libgomp1              : PaddleOCR OpenMP 병렬 처리
#
# apt 리스트는 여기서 제거하지 않는다.
# playwright install --with-deps 가 apt-get 을 내부적으로 호출하므로
# lists 가 필요하다. 정리는 playwright 설치 레이어에서 일괄 수행.
# ─────────────────────────────────────────────────────────────────
RUN apt-get update && apt-get install -y --no-install-recommends \
    libgl1 \
    libglib2.0-0 \
    libgomp1

WORKDIR /app

# ─────────────────────────────────────────────────────────────────
# Python 의존성 (소스 복사 전 레이어 분리 → 캐시 재사용)
# ─────────────────────────────────────────────────────────────────
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# ─────────────────────────────────────────────────────────────────
# Embedding 모델 사전 다운로드
#  런타임 네트워크 없이 모델 로드 가능
# ─────────────────────────────────────────────────────────────────
ARG EMBEDDING_MODEL=jhgan/ko-sroberta-multitask
RUN python -c "from sentence_transformers import SentenceTransformer; SentenceTransformer('${EMBEDDING_MODEL}')"

# ─────────────────────────────────────────────────────────────────
# Playwright Chromium + 시스템 의존성 설치, apt 정리
#  --with-deps: Chromium 바이너리 + libnss3 등 apt 패키지를 한 번에 처리
#  같은 RUN 레이어에서 apt 리스트를 제거해 이미지 크기 최소화
# ─────────────────────────────────────────────────────────────────
RUN playwright install --with-deps chromium \
    && rm -rf /var/lib/apt/lists/*

# ─────────────────────────────────────────────────────────────────
# PaddleOCR 한국어 모델 사전 다운로드
#  HOME=/app 으로 설정 → 모델이 /app/.paddleocr 에 저장됨
#  런타임 비루트 사용자(home=/app)와 경로 일치 → 기동 시 네트워크 불필요
# ─────────────────────────────────────────────────────────────────
RUN HOME=/app python -c \
    "from paddleocr import PaddleOCR; PaddleOCR(lang='korean', use_angle_cls=True)"

# ─────────────────────────────────────────────────────────────────
# 소스 코드 복사
# ─────────────────────────────────────────────────────────────────
COPY src/ ./src/

# ─────────────────────────────────────────────────────────────────
# gRPC proto 코드 생성
#  PYTHONPATH=/app/src 기준으로 ocr/grpc 패키지에 pb2 파일 생성
# ─────────────────────────────────────────────────────────────────
RUN python -m grpc_tools.protoc \
    --proto_path=/app/src \
    --python_out=/app/src \
    --grpc_python_out=/app/src \
    /app/src/ocr/grpc/ocr_service.proto

# ─────────────────────────────────────────────────────────────────
# 비루트 사용자 설정
#  -d /app : HOME=/app → PaddleOCR 모델 경로(/app/.paddleocr)와 일치
#  소유권 이전 후 USER 전환
# ─────────────────────────────────────────────────────────────────
RUN useradd -r -u 1001 -d /app -s /sbin/nologin appuser \
    && chown -R appuser:appuser /app

USER appuser
ENV HOME=/app

# ─────────────────────────────────────────────────────────────────
# 진입점 : Kafka 기반 전처리 파이프라인
#
# 런타임 필수 환경 변수 (docker run -e 또는 compose env_file):
#   KAFKA_BOOTSTRAP_SERVERS  - 브로커 주소     (기본: 127.0.0.1:19092)
#   KAFKA_USERNAME           - SASL 사용자     (SASL_PLAINTEXT 시 필수)
#   KAFKA_PASSWORD           - SASL 비밀번호   (SASL_PLAINTEXT 시 필수)
#   KAFKA_SECURITY_PROTOCOL  - 기본: SASL_PLAINTEXT
#   KAFKA_CONSUMER_GROUP     - 기본: preprocess-group
#   KAFKA_TEXT_TOPIC         - 기본: hirelog.outbox.JdPreprocessText
#   KAFKA_OCR_TOPIC          - 기본: hirelog.outbox.JdPreprocessOcr
#   KAFKA_URL_TOPIC          - 기본: hirelog.outbox.JdPreprocessUrl
#   KAFKA_RESULT_TOPIC       - 기본: jd.preprocess.response
#   KAFKA_FAIL_TOPIC         - 기본: jd.preprocess.response.fail
#   OCR_GRPC_PORT            - OCR ↔ TEXT/URL 프로세스 간 gRPC 포트 (기본: 50051)
#   OCR_GRPC_HOST            - OCR gRPC 서버 호스트 (기본: localhost, 동일 컨테이너)
# ─────────────────────────────────────────────────────────────────
EXPOSE 8000

CMD ["python", "src/main_kafka.py"]
