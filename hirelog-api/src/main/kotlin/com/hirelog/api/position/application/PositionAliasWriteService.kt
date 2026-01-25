package com.hirelog.api.position.application

import com.hirelog.api.position.application.port.PositionAliasCommand
import com.hirelog.api.position.application.port.PositionAliasLoad
import com.hirelog.api.position.application.port.PositionLoad
import com.hirelog.api.position.domain.PositionAlias
import com.hirelog.api.position.domain.PositionStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PositionAliasWriteService(
    private val positionAliasLoad: PositionAliasLoad,
    private val positionAliasCommand: PositionAliasCommand,
    private val positionLoad: PositionLoad
) {

    /**
     * Pending 상태 Alias 생성 (자동 생성)
     */
    @Transactional
    fun createPending(
        aliasName: String,
        positionId: Long
    ): Long {
        val position = loadActivePosition(positionId)
        val normalizedAliasName = PositionAlias.normalize(aliasName)

        val alias = PositionAlias.createPending(aliasName, position)

        return saveWithDuplicateHandling(alias, normalizedAliasName)
    }

    /**
     * Active 상태 Alias 생성 (관리자)
     */
    @Transactional
    fun createActive(
        aliasName: String,
        positionId: Long
    ): Long {
        val position = loadActivePosition(positionId)
        val normalizedAliasName = PositionAlias.normalize(aliasName)

        val alias = PositionAlias.createActive(aliasName, position)

        return saveWithDuplicateHandling(alias, normalizedAliasName)
    }

    /**
     * Alias 승인
     */
    @Transactional
    fun approve(aliasId: Long) {
        val alias = loadRequiredAlias(aliasId)
        alias.approve()
    }

    /**
     * Alias 비활성화
     */
    @Transactional
    fun deactivate(aliasId: Long) {
        val alias = loadRequiredAlias(aliasId)
        alias.deactivate()
    }

    /**
     * Active Position 로딩
     */
    private fun loadActivePosition(positionId: Long) =
        positionLoad.loadById(positionId)
            ?.takeIf { it.status == PositionStatus.ACTIVE }
            ?: throw IllegalArgumentException("Active Position not found: $positionId")

    /**
     * Alias 필수 로딩
     */
    private fun loadRequiredAlias(aliasId: Long): PositionAlias =
        positionAliasLoad.loadById(aliasId)
            ?: throw IllegalArgumentException("PositionAlias not found: $aliasId")

    /**
     * 중복 처리 포함 저장
     *
     * 전략:
     * - DB unique index 신뢰
     * - 충돌 시 재조회
     */
    private fun saveWithDuplicateHandling(
        alias: PositionAlias,
        normalizedAliasName: String
    ): Long {
        return try {
            positionAliasCommand.save(alias).id
        } catch (ex: Exception) {
            positionAliasLoad.loadByNormalizedAliasName(normalizedAliasName)
                ?.id
                ?: throw ex
        }
    }
}

