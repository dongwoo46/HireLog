# hirelog-api 코딩 컨벤션

## 엔티티 설계 패턴

```kotlin
@Entity
class Foo protected constructor(   // protected constructor 필수

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,

    // 불변 필드: val + updatable = false
    @Column(nullable = false, updatable = false)
    val ownerId: Long,

    // 가변 필드: var
    var status: FooStatus

) : BaseEntity() {   // 또는 VersionedEntity (낙관적 락 필요 시)

    // 상태 변경은 도메인 메서드로만
    fun doSomething() {
        require(status == FooStatus.ACTIVE) { "..." }
        status = FooStatus.DONE
    }

    // 팩토리 메서드
    companion object {
        fun create(ownerId: Long, ...): Foo {
            require(ownerId > 0) { "..." }
            return Foo(ownerId = ownerId, ...)
        }
    }
}
```

### BaseEntity vs VersionedEntity

| 클래스 | 용도 | 필드 |
|---|---|---|
| `BaseEntity` | 일반 엔티티 | `createdAt`, `updatedAt` |
| `VersionedEntity` | 낙관적 락 필요 (동시 수정 가능성) | `createdAt`, `updatedAt`, `@Version` |

- `Member`, `JobSummary` → `VersionedEntity`
- `JobSummaryReview`, `Report`, `Board`, `Comment` → `BaseEntity`

---

## Soft Delete 패턴

```kotlin
@Column(name = "deleted", nullable = false)
var deleted: Boolean = false

fun softDelete() {
    require(!deleted) { "이미 삭제된 엔티티입니다" }
    deleted = true
}

fun isDeleted(): Boolean = deleted
```

- 물리 삭제 금지 (감사 추적, 신고 대상 보존)
- 조회 포트에서 `includeDeleted: Boolean` 파라미터로 필터 제어
- QueryDSL: `entity.deleted.isFalse` 조건 추가

---

## nullable FK 다형성 패턴 (Report)

여러 엔티티를 참조하는 신고/이력 테이블에서 `targetType + targetId` polymorphic 방식 금지.
→ nullable FK 컬럼 분리 사용.

```kotlin
val jobSummaryId: Long? = null
val boardId: Long? = null
// ...

val targetType: TargetType
    get() = when {
        jobSummaryId != null -> TargetType.JOB_SUMMARY
        boardId != null -> TargetType.BOARD
        else -> throw IllegalStateException()
    }
```

- DB FK constraint 적용 가능
- 중복 방지: PostgreSQL partial unique index 필수
- 단일 대상 강제: `listOfNotNull(...).size == 1` 검증 + DB CHECK constraint

---

## Command 포트 메서드 명명

```kotlin
interface FooCommand {
    fun save(foo: Foo): Foo
    fun findById(id: Long): Foo?
    fun existsBy{Condition}(...): Boolean
    fun findBy{Condition}AndStatus(...): Foo?
}
```

---

## Response DTO 변환

```kotlin
data class FooRes(...) {
    companion object {
        fun from(view: FooView) = FooRes(...)
    }
}
```

- View → Res 변환은 `from(view)` 팩토리 패턴
- 익명 처리: `if (view.anonymous) null else view.authorUsername`
- 삭제된 댓글 내용: `if (view.deleted) "(삭제된 댓글입니다)" else view.content`

---

## 로그 패턴

```kotlin
log.info("[{DOMAIN}_{ACTION}] key1={}, key2={}", val1, val2)
```

예시:
- `[BOARD_CREATED] id={}, memberId={}`
- `[REPORT_RESOLVED] id={}, adminMemberId={}`
- `[JD_PREPROCESS_FAIL_HANDLED] requestId={}, errorCode={}`

---

## 중복 방지 검증 순서

WriteService에서 저장 전 중복 검증:
1. Application-level `existsBy...()` 체크 (빠른 실패)
2. DB-level unique constraint / partial unique index (최종 방어선)

두 레이어 모두 필요. Application-level만으로는 멀티 인스턴스 환경에서 race condition 발생 가능.