# .claude 디렉토리 구조

```
.claude/
├── rules/                          ← Claude Code 자동 로드 규칙
│   ├── architecture.md             ← 헥사고날/CQRS 레이어, 트랜잭션 정책
│   └── coding-conventions.md      ← 엔티티 패턴, 네이밍, 로그 포맷
│
├── features/                       ← 기능별 설계 결정 기록
│   ├── notification-failure-title.md          ← 실패 알림 title에 brandName/positionName 추가
│   └── jd-summary-processing-lifecycle.md     ← JdSummaryProcessing 생성 시점 (intake로 이동)
│
└── troubleshooting/                ← 고민한 문제와 결론
    └── kafka-idempotency-strategy.md          ← 컨슈머별 멱등성 전략 분리 (at-most-once vs at-least-once)
```

## features 작성 기준
- 왜 이렇게 설계했는지 (배경)
- 어떤 파일이 바뀌었는지
- 연관된 다른 결정이 있으면 cross-reference

## troubleshooting 작성 기준
- 문제 현상
- 검토한 옵션들과 트레이드오프
- 채택한 결론과 근거
- 완전한 해결책이 아닌 경우 미적용 이유 명시