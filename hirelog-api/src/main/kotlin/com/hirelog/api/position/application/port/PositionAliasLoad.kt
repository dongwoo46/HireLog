package com.hirelog.api.position.application.port

import com.hirelog.api.position.domain.PositionAlias

/**
 * PositionAliasLoad
 *
 * 책임:
 * - Write 유스케이스를 위한 PositionAlias 조회
 *
 * 사용 목적:
 * - Alias 중복 여부 판단
 * - 기존 Alias 재사용 판단
 *
 * 특징:
 * - Entity 반환
 * - 내부 도메인 전용
 *
 * 금지:
 * - 목록 조회 ❌
 * - View 반환 ❌
 * - 외부 API 노출 ❌
 */
interface PositionAliasLoad {

    /**
     * ID 기준 단건 조회
     *
     * 용도:
     * - approve / deactivate
     * - 상태 전이 대상 로딩
     */
    fun loadById(aliasId: Long): PositionAlias?

    /**
     * 정규화된 Alias 명칭 기준 단건 조회
     *
     * 용도:
     * - Alias 중복 생성 방지
     * - 기존 Alias 존재 여부 확인
     */
    fun loadByNormalizedAliasName(
        normalizedAliasName: String
    ): PositionAlias?
}
