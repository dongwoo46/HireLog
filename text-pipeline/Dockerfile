FROM python:3.10-slim

# PaddleOCR 내부 로그(glog, gflags) 최소화

ENV GLOG_minloglevel=3
ENV FLAGS_minloglevel=3
ENV FLAGS_use_mkldnn=False
ENV FLAGS_use_mkldnn=0
ENV DISABLE_MODEL_SOURCE_CHECK=True

# PaddleOCR / OpenCV 실행에 필요한 시스템 라이브러리
RUN apt-get update && apt-get install -y \
    libgl1 \
    libglib2.0-0 \
    libgomp1 \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Python 의존성 설치
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# 코드 복사
COPY . .

# OCR 디버그 스크립트 실행
CMD ["python", "scripts/debug_ocr.py"]
