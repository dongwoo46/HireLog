package com.hirelog.api.position.application

import com.hirelog.api.common.exception.EntityAlreadyExistsException
import com.hirelog.api.common.exception.EntityNotFoundException
import com.hirelog.api.common.logging.log
import com.hirelog.api.position.application.port.PositionCommand
import com.hirelog.api.position.application.port.PositionLoad
import com.hirelog.api.position.domain.Position
import com.hirelog.api.position.domain.PositionCategory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Position Write Application Service
 *
 * 책임:
 * - Position 생성 및 상태 변경 유스케이스 실행
 * - 트랜잭션 경계 정의
 *
 * 설계 원칙:
 * - Read(Query/View) 포트 의존 ❌
 * - Write 판단은 Load(Entity) 포트만 사용
 */
@Service
class PositionWriteService(
    private val positionCommand: PositionCommand,
    private val positionLoad: PositionLoad
) {

    /**
     * Position 확보 (idempotent)
     *
     * 정책:
     * - normalizedName 기준 단일 Position 보장
     * - 존재하면 그대로 반환
     * - 없으면 CANDIDATE 상태로 신규 생성
     * - 동시성은 DB unique + 재조회로 해결
     */
    @Transactional
    fun getOrCreate(
        name: String,
        positionCategory: PositionCategory,
        normalizedName: String
    ): Position {

        // ── [LOG] getOrCreate 진입 ────────────────────────────────
        log.info(
            "[POSITION_GET_OR_CREATE_ENTER] name={}, normalizedName={}, categoryId={}, thread={}",
            name,
            normalizedName,
            positionCategory.id,
            Thread.currentThread().name
        )

        positionLoad.loadByNormalizedName(normalizedName)?.let {
            return it
        }

        log.info(
            "[POSITION_GET_OR_CREATE_TRY_SAVE] normalizedName={}, thread={}",
            normalizedName,
            Thread.currentThread().name
        )

        val position = Position.create(
            name = name,
            positionCategory = positionCategory,
            description = null
        )

        return try {
            val saved = positionCommand.save(position)

            log.info(
                "[POSITION_GET_OR_CREATE_SAVED] normalizedName={}, positionId={}, thread={}",
                normalizedName,
                saved.id,
                Thread.currentThread().name
            )

            saved
        } catch (ex: DataIntegrityViolationException) {
            positionLoad.loadByNormalizedName(normalizedName)
                ?: throw ex
        }
    }

    /**
     * Position 신규 생성
     *
     * 정책:
     * - normalizedName 중복 불가
     * - 동시성은 DB unique 제약 + 예외 변환으로 처리
     */
    @Transactional
    fun create(
        name: String,
        normalizedName: String,
        positionCategory: PositionCategory,
        description: String?
    ): Position {

        val position = Position.create(
            name = name,
            description = description,
            positionCategory = positionCategory
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

    /**
     * Position 활성화
     */
    @Transactional
    fun activate(positionId: Long) {
        getRequired(positionId).activate()
    }

    /**
     * Position 비활성화 (Deprecated)
     */
    @Transactional
    fun deprecate(positionId: Long) {
        getRequired(positionId).deprecate()
    }

    /**
     * 필수 Position 조회
     *
     * 정책:
     * - 존재하지 않으면 예외
     */
    private fun getRequired(positionId: Long): Position =
        positionLoad.loadById(positionId)
            ?: throw EntityNotFoundException("Position", positionId)
}
