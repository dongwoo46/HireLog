# HireLog Web

HireLog 프론트엔드(React + TypeScript + Vite) 프로젝트입니다.

## 실행

```bash
npm install
npm run dev
```

기본 개발 서버: `http://localhost:5173`

## 빌드

```bash
npm run build
```

## 주요 화면

- `/jd`: 공고 목록/검색
- `/jd/:id`: 공고 상세
- `/my-jobs`: 내 공고(저장한 공고 / 지원한 공고)
- `/boards`: 게시판
- `/login`, `/signup`: 로그인/회원가입

## 최근 반영 사항

- 회원가입 문구 통일: `일반 회원가입` -> `회원가입`
- 회원가입 닉네임 중복확인 추가
  - API: `POST /api/auth/signup/general/check-username`
  - UI: 회원가입 폼에서 `중복확인` 후 가입 진행
- 내 공고(`/my-jobs`) 필터 상태 유지
  - 저장 위치: `zustand persist (localStorage)`
  - 유지 항목: 탭, 브랜드 검색어, 지원한 공고 stage/result 필터
- 게시판 뱃지 정리
  - `notice` 표시: `공지사항`
  - `pinned` 표시: 민트 컬러 핀 아이콘
  - 목록 카드에 작성일(`YYYY-MM-DD`) 표시
- 헤더 반응형 정리
  - 모바일(햄버거 구간)에서는 상단 로그아웃 텍스트 숨김
  - 모바일 메뉴 내 로그아웃 사용

## 상태 저장 스토어

- 인증: `src/store/authStore.ts`
- 내 공고 필터: `src/store/myJobsFilterStore.ts`

