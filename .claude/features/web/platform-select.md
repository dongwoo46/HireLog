# 채용 플랫폼 선택 UI

**작업일**: 2026-03-30
**페이지**: JobSummaryRequestPage (`/jd/request`)

---

## 변경 내용

JD 요약 요청 폼에 채용 플랫폼 선택 드롭다운 추가.

### 위치
회사명 / 포지션명 입력 필드 아래, 탭(텍스트/OCR/URL) 위

### UI 구성
```tsx
<select value={platform} onChange={...}>
  {PLATFORM_OPTIONS.map(([value, label]) => (
    <option key={value} value={value}>{label}</option>
  ))}
</select>
```

### 지원 플랫폼 (JOB_PLATFORM_LABELS)
원티드, 리멤버, 사람인, 잡코리아, 로켓펀치, 프로그래머스,
점핏, 랠릿, 캐치, 인크루트, 그렙, 링크드인, 기타

---

## 수정 파일

| 파일 | 변경 |
|---|---|
| `src/types/jobSummary.ts` | `JobPlatformType` union type 추가, `JOB_PLATFORM_LABELS` Record 추가, `JobSummaryTextReq`·`JobSummaryUrlReq`에 `platform` 필드 추가 |
| `src/pages/JobSummaryRequestPage.tsx` | `platform` state 추가, 드롭다운 UI 추가, 각 submit 시 platform 전달 |
| `src/services/jdSummaryService.ts` | `requestOcr` FormData에 `platform` 추가 |

---

## 기본값

`OTHER` (기타) — 선택 안 해도 요청 가능

---

## API 연동

- Text/URL: `{ ..., platform: "WANTED" }` JSON body
- OCR: FormData에 `platform` 필드 추가 (`formData.append('platform', data.platform)`)
