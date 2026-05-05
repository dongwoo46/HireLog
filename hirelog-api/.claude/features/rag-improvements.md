# RAG 고도화 기록

## 2026-04-16 — STATISTICS aggregation 개선

### 배경
"서류합격한 곳의 특징은?" 질문에 대해 기술스택(Git, Docker 등) 나열 위주의 답변이 반환되는 품질 문제.

### 변경 내용

**기술스택 aggregation 조건부 처리**
- `RagQuery`에 `focusTechStack: Boolean` 필드 추가
- Parser가 질문에 특정 기술명이 명시될 때만 `true` 설정
  - true 예시: "spring 쓰는 곳 얼마나 합격했어?"
  - false 예시: "서류합격한 곳 특징은?", "저장한 공고 공통점은?"
- `buildAggregationEntries`에서 `focusTechStack=false`이면 techStack 버킷 제외

**aggregation 카테고리 추가**
- `careerType` — 신입/경력/무관 분포
- `positionCategory` — 직군 분포 (백엔드, 프론트엔드, 데이터 등)
  - OpenSearch: `positionCategoryName.keyword` 필드로 terms 집계

**Composer 프롬프트 수정**
- textFeatures를 주답변으로 앞에 배치
- techStack 항목은 `focusTechStack=true`일 때(질문에 기술명 명시)만 인용하도록 지시
- careerType / positionCategory / companyDomain / companySize는 의미 있을 때 보조로 언급

### 수정 파일
- `RagQuery.kt` — focusTechStack 필드 추가
- `GeminiRagParserAdapter.kt` — focusTechStack 파싱
- `GeminiPromptBuilder.kt` — Parser/Composer 지시문 수정
- `RagQueryExecutor.kt` — buildAggregationEntries 조건 분기
- `JobSummaryOpenSearchAdapter.kt` — careerTypes, positionCategories aggregation 추가