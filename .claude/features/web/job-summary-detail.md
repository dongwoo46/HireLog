# 채용 공고 상세 페이지

**작업일**: 2026-03-30
**페이지**: JobSummaryDetailPage (`/jd/:id`)

---

## 탭 구성

| 탭 | 조건 | 데이터 |
|---|---|---|
| 상세 정보 | 항상 | `GET /job-summary/{id}` |
| 리뷰 | 로그인 시 활성 | `GET /job-summary/review/{id}` |
| 준비 기록 | 로그인 시 활성 | `GET /member-job-summary/{id}/stages` + `GET /member-job-summary/{id}/cover-letters` (병렬) |

---

## 상세 정보 탭 렌더링 필드

`JobSummaryDetailView`에서 수신하는 모든 필드를 3개 섹션으로 구분하여 표시.

### 공고 핵심 정보
| 필드 | UI |
|---|---|
| `summaryText` | Block (요약) |
| `responsibilities` | Block (주요 업무) |
| `requiredQualifications` | Block (자격 요건) |
| `preferredQualifications` | Block (우대 사항) |
| `techStack` / `techStackParsed` | 태그 뱃지 (TechStackBlock) |
| `recruitmentProcess` | 번호+화살표 스텝 (RecruitmentProcessBlock) |
| `technicalContext` | Block (기술 맥락) |
| `keyChallenges` | Block (핵심 도전과제) |

### 지원 전략
| 필드 | UI |
|---|---|
| `idealCandidate` | Block (이상적인 후보자) |
| `mustHaveSignals` | Block (필수 신호) |
| `considerations` | Block (고려사항) |
| `preparationFocus` | Block (준비 포인트) |
| `proofPointsAndMetrics` | Block + proofMetricsLayout (증명 포인트) |
| `transferableStrengthsAndGapPlan` | Block (강점 및 보완점, [강점]/[보완] bracket 포맷) |

### 면접 준비
| 필드 | UI |
|---|---|
| `storyAngles` | Block (스토리 앵글) |
| `insights` | Block (인사이트) |
| `questionsToAsk` | Block (면접 질문) |

### 공통
- `sourceUrl`: 상단 "원본 공고 보기" 링크 버튼 (있을 때만)
- 모든 Block은 content가 null/undefined면 자동으로 렌더링 생략

---

## Block 컴포넌트

```
content가 없으면 → null 반환 (자동 생략)
줄바꿈 기준 라인 분리
- [bracket] 형식 → 색상 라벨 + 내용 2열 렌더링
- 일반 라인 → 불릿 리스트
- highlightKeywords(): 기술 키워드 강조
useProofMetricsLayout: [증거]/[지표] 파싱하여 2그룹으로 렌더링
```

---

## 타입 (JobSummaryDetailView)

`types/jobSummary.ts`에 정의.
API 응답 기준 전체 필드:

```typescript
// 기본 (JobSummaryView 상속)
summaryId, brandName, brandPositionName, positionName,
positionCategoryName, careerType, summaryText, techStackParsed,
thumbnailUrl, createdAt, isSaved, memberJobSummaryId, memberSaveType

// 상세
responsibilities, requiredQualifications, preferredQualifications,
techStack, recruitmentProcess, sourceUrl, insights,
preparationFocus, proofPointsAndMetrics, questionsToAsk,

// AI 분석
idealCandidate, mustHaveSignals, transferableStrengthsAndGapPlan,
storyAngles, keyChallenges, technicalContext, considerations
```
