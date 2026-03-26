# 전체 데이터 흐름 및 아키텍처

## 입력 DTO

```python
@dataclass
class JdPreprocessInput:
    request_id: str
    brand_name: str
    position_name: str
    source: str          # "URL" | "IMAGE" | "TEXT"
    created_at: int
    message_version: str
    text: Optional[str]
    images: Optional[list]
    url: Optional[str]
```

## 출력 DTO (canonical_map 핵심)

```python
@dataclass
class JdPreprocessOutput:
    canonical_map: dict         # zone → list[str]
    recruitment_period_type: str
    recruitment_open_date: str
    recruitment_close_date: str
    skills: list[str]
```

## canonical_map zones

| zone | 의미 | Spring 검증 |
|---|---|---|
| `responsibilities` | 주요업무 | null check |
| `requirements` | 자격요건 | null check |
| `preferred` | 우대사항 | null check ← 자주 누락 |
| `process` | 채용절차 | - |
| `company` | 회사소개 | - |
| `benefits` | 복지 | - |
| `skills` | 기술스택 | - |
| `experience` | 경력 | - |
| `employment_type` | 고용형태 | - |
| `location` | 근무지 | - |
| `application_guide` | 지원 가이드 | - |
| `application_questions` | 지원서 문항 | - |
| `intro` | 도입부 | - |
| `others` | 미분류 | - |

## Spring isValidJd() 차단 조건

```kotlin
canonicalText.isBlank()                             // 전체 섹션 비어 있음
canonicalText.length < 300                          // 텍스트 총량 부족
canonicalMap["responsibilities"].isNullOrEmpty()    // 업무 섹션 누락
canonicalMap["requirements"].isNullOrEmpty()        // 자격요건 섹션 누락
canonicalMap["preferred"].isNullOrEmpty()           // 우대사항 섹션 누락 ← 핵심 이슈
```

## 메시지 브로커

- **Redis Stream** (구버전): `jd:preprocess:url:request:stream`
- **Kafka** (신버전): `kafka_jd_preprocess_url_worker.py`

## Worker 구조

```
BaseWorker (worker/base_worker.py or worker/base_kafka_worker.py)
  ├─ UrlStreamWorker  → JdPreprocessUrlWorker  → UrlPipeline
  ├─ TextStreamWorker → JdPreprocessTextWorker → TextPreprocessPipeline
  └─ OcrStreamWorker  → JdPreprocessOcrWorker  → OcrPipeline
```

## 공통 후처리 파이프라인

모든 파이프라인이 Section 목록으로 수렴한 후 공통 처리:

```
list[Section]
 └─ CanonicalSectionPipeline.process()
     ├─ apply_semantic_lite()
     │   └─ detect_semantic_zone(header) → semantic_zone 태깅
     ├─ filter_irrelevant_sections()
     │   └─ DROP: 유의사항, 마감일, 참고사항, 안내사항, 기타사항, notice, disclaimer
     └─ _build_canonical_map()
         └─ zone별로 lines + lists 항목 합산
```
