package com.hirelog.api.relation.domain.type

/**
 * 채용 단계 결과 (선택적)
 *
 * 의미:
 * - 사용자가 알고 있는 경우에만 기록
 * - 모르면 null 유지
 */
enum class HiringStageResult {
    PASSED,
    FAILED,
    PENDING
}
