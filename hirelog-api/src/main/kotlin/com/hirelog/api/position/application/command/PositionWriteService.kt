package com.hirelog.api.position.application.command

import com.hirelog.api.common.exception.EntityAlreadyExistsException
import com.hirelog.api.common.exception.EntityNotFoundException
import com.hirelog.api.position.application.query.PositionQuery
import com.hirelog.api.position.domain.Position
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Position Write Application Service
 *
 * 책임:
 * - Position 변경 유스케이스 실행
 * - 트랜잭션 경계 정의
 *
 * 주의:
 * - 조회 판단 ❌
 * - 순수 Write만 담당
 */
@Service
class PositionWriteService(
    private val positionCommand: PositionCommand,
    private val positionQuery: PositionQuery
) {

    /**
     * Position 생성
     *
     * 정책:
     * - normalizedName 중복 불가
     * - 동시성은 DB unique + 예외 변환으로 처리
     */
    @Transactional
    fun create(
        name: String,
        normalizedName: String,
        description: String?
    ): Position {

        val position = Position.create(
            name = name,
            normalizedName = normalizedName,
            description = description
        )

        return try {
            positionCommand.save(position)
        } catch (ex: DataIntegrityViolationException) {
            throw EntityAlreadyExistsException(
                "Position already exists. normalizedName=$normalizedName",
                ex
            )
        }
    }

    @Transactional
    fun activate(positionId: Long) {
        getRequired(positionId).activate()
    }

    @Transactional
    fun deprecate(positionId: Long) {
        getRequired(positionId).deprecate()
    }

    private fun getRequired(positionId: Long): Position =
        positionQuery.findById(positionId)
            ?: throw EntityNotFoundException("Position", positionId)
}

