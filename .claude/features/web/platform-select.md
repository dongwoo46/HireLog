# 채용 플랫폼 선택 UI

**작업일**: 2026-03-30

---

## 현재 상태

platform 입력은 **URL 탭에만** 존재. TEXT / OCR 탭에는 없음.

### 위치
`JobSummaryRequestPage` → URL 탭 내부 (URL 입력 아래)

### UI 구성
```tsx
{activeTab === 'url' && (
  <div className="space-y-4">
    <input type="url" ... />
    <select value={platform} onChange={...}>
      {PLATFORM_OPTIONS.map(...)}
    </select>
  </div>
)}
```

### 지원 플랫폼 (JOB_PLATFORM_LABELS)
원티드, 리멤버, 사람인, 잡코리아, 로켓펀치, 프로그래머스,
점핏, 랠릿, 캐치, 인크루트, 그렙, 링크드인, 기타

---

## 수정 파일

| 파일 | 변경 |
|---|---|
| `src/types/jobSummary.ts` | `JobSummaryTextReq`에서 `platform` 제거 |
| `src/pages/JobSummaryRequestPage.tsx` | 공통 platform select 제거 → URL 탭 내부로 이동, TEXT/OCR submit에서 platform 제거 |
| `src/services/jdSummaryService.ts` | `requestOcr` FormData에서 `platform` 제거, `requestOcr` 파라미터 타입에서 `platform` 제거 |

---

## 기본값

`OTHER` (기타) — `platform` state는 컴포넌트 전체에 유지되나 URL 탭에서만 사용

---

## API 연동

- URL: `{ ..., platform: "WANTED" }` JSON body
- TEXT: platform 필드 없음
- OCR: platform 필드 없음 (FormData에 미포함)
