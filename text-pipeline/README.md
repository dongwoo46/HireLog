
### 최초 1회
docker build -t hirelog-ocr-dev .
### 이후에는 이것만
docker run --rm \
-v $(pwd)/data:/app/data \
-v $(pwd):/app \
hirelog-ocr-dev

## 🧾 OCR → JD 텍스트 처리 파이프라인

본 시스템은 OCR 결과를 그대로 사용하지 않고,
Job Description(JD)에 적합한 형태로 **단계적으로 정제·구조화** 한다.

---

##  처리 순서

### 1️. OCR Raw 추출

- 이미지 / PDF에서 OCR로 텍스트 추출
- 자간 분리, 노이즈, 낮은 신뢰도가 포함된 원본 상태

### 2️. OCR 품질 기반 노이즈 제거

- 평균 confidence가 낮은 라인 제거
- 숫자 또는 특수문자만 있는 라인 제거

### 3. 문자 단위 정규화

- 유니코드 정규화 (NFKC)
- 공백 정리
- 한글 자간 분리 병합

### 4. 토큰 단위 정규화

- OCR 오인식 기술 키워드 보정
    - 예: `qrpc → gRPC`

### 5️. 라인 단위 정규화

- 의미 없는 단일 토큰 라인 제거

### 6️. JD 의미 기반 노이즈 제거

- `지원하기`, `apply`, `privacy` 등

  UI / 메타 / boilerplate 문구 제거


### 7️. 라인 타입 분류

- `header` / `bullet` / `body` / `noise`
- `header`는 보수적으로만 판단

### 8️. 섹션 컨텍스트 빌딩

- header 기준으로 JD를 섹션별로 묶음
- header 이전 라인은 `summary` 섹션으로 처리
- 섹션 키워드는 `section_keywords.yml` 사용

### 9️. JD 도메인 후처리

- LLM 요약 입력을 위한 최종 구조 생성

---

### 🎯 핵심 원칙
- OCR 결과는 신뢰하지 않는다
- 단계별 책임을 명확히 분리한다
- 애매한 경우는 보수적으로 처리한다
- LLM에는 정제된 JD만 전달한다