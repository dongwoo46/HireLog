package com.hirelog.api.common.exception


/**
 * 공통 Entity Already Exists 예외
 *
 * 의미:
 * - 생성/등록 시 이미 엔티티가 존재하는 경우
 * - DB unique constraint 위반을 도메인 의미로 번역
 *
 * HTTP 매핑:
 * - 409 Conflict
 *
 * 사용 예:
 * - CompanyRelation 중복 생성
 * - MemberBrand 중복 등록
 * - MemberCompany 중복 관심 등록
 */
class EntityAlreadyExistsException(
    entityName: String,
    identifier: Any,
    cause: Throwable? = null
) : RuntimeException(
    "$entityName already exists. identifier=$identifier",
    cause
)
