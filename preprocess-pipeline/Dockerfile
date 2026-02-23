FROM python:3.10-slim

# ─────────────────────────────────────────────────────────────────
# 공통 환경 변수
# ─────────────────────────────────────────────────────────────────
ENV PYTHONUNBUFFERED=1 \
    PYTHONDONTWRITEBYTECODE=1 \
    # src/ 하위 패키지 전체를 import 루트로 등록
    PYTHONPATH=/app/src \
    # PaddleOCR / PaddlePaddle C++ 내부 로그 억제
    GLOG_minloglevel=3 \
    FLAGS_minloglevel=3 \
    FLAGS_use_mkldnn=0 \
    DISABLE_MODEL_SOURCE_CHECK=True \
    # Playwright 브라우저 저장 경로 고정
    # (비루트 사용자로 전환 후에도 경로 불변)
    PLAYWRIGHT_BROWSERS_PATH=/app/.playwright

# ─────────────────────────────────────────────────────────────────
# 시스템 의존성
#  - libgl1, libglib2.0-0  : OpenCV headless 필수
#  - libgomp1              : PaddleOCR OpenMP 병렬 처리
#
# Playwright Chromium 의존성(libnss3 등)은
# playwright install --with-deps 가 자동으로 설치
# ─────────────────────────────────────────────────────────────────
RUN apt-get update && apt-get install -y --no-install-recommends \
    libgl1 \
    libglib2.0-0 \
    libgomp1 \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# ─────────────────────────────────────────────────────────────────
# Python 의존성 (소스 복사 전에 레이어 분리 → 캐시 재사용)
# ─────────────────────────────────────────────────────────────────
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# ─────────────────────────────────────────────────────────────────
# Playwright Chromium 설치
#  --with-deps : 브라우저 바이너리 + apt 시스템 의존성 패키지를 한 번에 처리
# ─────────────────────────────────────────────────────────────────
RUN playwright install --with-deps chromium

# ─────────────────────────────────────────────────────────────────
# PaddleOCR 한국어 모델 사전 다운로드
#  HOME=/app 으로 강제 설정 → 모델이 /app/.paddleocr 에 저장됨
#  비루트 사용자(home=/app)가 런타임에 동일 경로에서 모델을 찾을 수 있음
#  빌드 시 다운로드 완료 → 컨테이너 기동 시 네트워크 의존 없음
# ─────────────────────────────────────────────────────────────────
RUN HOME=/app python -c \
    "from paddleocr import PaddleOCR; PaddleOCR(lang='korean')"

# ─────────────────────────────────────────────────────────────────
# 소스 코드 복사
# ─────────────────────────────────────────────────────────────────
COPY src/ ./src/

# ─────────────────────────────────────────────────────────────────
# 비루트 사용자 설정
#  -d /app : HOME=/app → PaddleOCR 모델 경로(/app/.paddleocr)와 일치
#  /app 전체 소유권을 appuser 로 이전
# ─────────────────────────────────────────────────────────────────
RUN useradd -r -u 1001 -d /app -s /sbin/nologin appuser \
    && chown -R appuser:appuser /app

USER appuser
ENV HOME=/app

# ─────────────────────────────────────────────────────────────────
# 진입점 : Kafka 기반 전처리 파이프라인
#
# 런타임 필수 환경 변수 (docker run -e 또는 docker-compose env_file):
#   KAFKA_BOOTSTRAP_SERVERS  - 브로커 주소 (기본: 127.0.0.1:19092)
#   KAFKA_USERNAME           - SASL 인증 사용자 (SASL_PLAINTEXT 시 필수)
#   KAFKA_PASSWORD           - SASL 인증 비밀번호 (SASL_PLAINTEXT 시 필수)
#   KAFKA_SECURITY_PROTOCOL  - 기본: SASL_PLAINTEXT
#   KAFKA_CONSUMER_GROUP     - 기본: preprocess-group
#   KAFKA_TEXT_TOPIC         - 기본: hirelog.outbox.JdPreprocessText
#   KAFKA_OCR_TOPIC          - 기본: hirelog.outbox.JdPreprocessOcr
#   KAFKA_URL_TOPIC          - 기본: hirelog.outbox.JdPreprocessUrl
#   KAFKA_RESULT_TOPIC       - 기본: jd.preprocess.response
#   KAFKA_FAIL_TOPIC         - 기본: jd.preprocess.response.fail
# ─────────────────────────────────────────────────────────────────
CMD ["python", "src/main_kafka.py"]
