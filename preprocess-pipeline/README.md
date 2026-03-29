
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

### 테스트 실행 코드
pip uninstall paddlepaddle paddleocr -y

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

---

## 언어 전환 고도화 초안 (Python -> Go/Rust)

현재 결론:
- 1차 전환은 `Go` 우선
- 2차 최적화는 병목 핫패스에 한해 `Rust` 선택 검토

선택 근거:
- 현재 파이프라인은 Kafka 워커 + 문자열 정규화 + I/O 혼합 처리 중심
- Go는 운영 단순화/개발 속도/성능 개선의 균형이 좋음
- Rust는 최고 성능 잠재력이 크지만 전면 전환 비용과 러닝커브가 큼

### 전환 범위 (권장 순서)
1. Text Worker (CPU-bound 문자열/정규화)
2. URL Worker (I/O-bound fetch + 파싱)
3. OCR Worker는 당분간 Python 유지 (OCR 의존성 영향)

### 단계별 계획
1. Baseline 측정
- 현재 Python 기준 throughput, p95 latency, CPU, memory, retry rate 수집

2. Go PoC
- Text/URL Worker를 Go로 최소 기능 구현
- Kafka consume/produce, fail topic, commit 정책 동일하게 유지

3. 병행 운영
- Python/Go 동시 운영 후 결과 정합성 비교
- 장애 시 즉시 Python 경로로 롤백 가능하게 유지

4. 최종 전환
- 성능/안정성 기준 충족 시 Go를 기본 경로로 승격
- 잔여 CPU 병목만 Rust 모듈 또는 Rust 서비스로 추가 최적화

### 성공 기준 (예시)
- throughput 2배 이상 개선
- p95 latency 30% 이상 감소
- retry/parse-fail rate 기존 대비 악화 없음
- 장애 복구 시간(MTTR) 단축
