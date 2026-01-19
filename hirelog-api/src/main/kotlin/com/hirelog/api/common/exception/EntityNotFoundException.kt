package com.hirelog.api.common.exception

/**
 * 공통 Entity Not Found 예외
 *
 * 사용 원칙:
 * - 외부 요청으로 조회한 엔티티가 존재하지 않을 때
 * - REST API에서는 404 Not Found로 매핑
 * - 도메인별 NotFoundException을 만들기 전까지 기본값으로 사용
 */
class EntityNotFoundException(
    entityName: String,
    identifier: Any
) : RuntimeException(
    "$entityName not found. identifier=$identifier"
)
