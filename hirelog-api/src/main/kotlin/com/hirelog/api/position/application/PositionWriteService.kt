package com.hirelog.api.position.application

import com.hirelog.api.common.exception.EntityAlreadyExistsException
import com.hirelog.api.common.exception.EntityNotFoundException
import com.hirelog.api.common.logging.log
import com.hirelog.api.common.utils.Normalizer
import com.hirelog.api.position.application.port.PositionCategoryCommand
import com.hirelog.api.position.application.port.PositionCommand
import com.hirelog.api.position.domain.Position
import com.hirelog.api.position.domain.PositionCategory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Position Write Application Service
 *
 * 책임:
 * - Position 생성 및 상태 변경 유스케이스 수행
 * - 트랜잭션 경계 정의
 */
@Service
class PositionWriteService(
    private val positionCommand: PositionCommand,
    private val positionCategoryCommand: PositionCategoryCommand
) {

    /**
     * Position 신규 생성 (관리자 전용)
     *
     * 정책:
     * - normalizedName 중복 불가
     * - 상태는 ACTIVE로 생성
     * - 동시성은 DB unique 제약으로 처리
     */
    @Transactional
    fun create(
        name: String,
        categoryId: Long,
        description: String?
    ): Position {

        val positionCategory =
            positionCategoryCommand.findById(categoryId)
                ?: throw EntityNotFoundException("PositionCategory", categoryId)

        val position = Position.create(
            name = name,
            positionCategory = positionCategory,
            description = description
        )

        return try {
            positionCommand.save(position)
        } catch (ex: DataIntegrityViolationException) {
            throw EntityAlreadyExistsException(
                "Position already exists. normalizedName=${position.normalizedName}",
                ex
            )
        }
    }

    /**
     * Position 활성화
     */
    @Transactional
    fun activate(positionId: Long) {
        val position = getRequired(positionId)
        position.activate()
        positionCommand.save(position)
    }

    /**
     * Position 비활성화 (Deprecated)
     */
    @Transactional
    fun deprecate(positionId: Long) {
        val position = getRequired(positionId)
        position.deprecate()
        positionCommand.save(position)
    }

    /**
     * 내부 전용: Position 확보 (idempotent)
     *
     * ⚠️ 주의:
     * - 외부 API에서 직접 호출 금지
     * - JD/LLM/크롤링 매핑용 유스케이스
     *
     * 정책:
     * - normalizedName 기준 단일 Position 보장
     * - 존재하면 반환
     * - 없으면 ACTIVE 상태로 생성
     * - 동시성은 DB unique + 재조회로 처리
     */
    @Transactional
    internal fun getOrCreate(
        name: String,
        positionCategory: PositionCategory
    ): Position {

        val normalizedName = Normalizer.normalizePosition(name)

        positionCommand.findByNormalizedName(normalizedName)?.let {
            return it
        }

        val position = Position.create(
            name = name,
            description = null,
            positionCategory = positionCategory
        )

        return try {
            positionCommand.save(position)
        } catch (ex: DataIntegrityViolationException) {
            positionCommand.findByNormalizedName(normalizedName)
                ?: throw ex
        }
    }

    /**
     * 필수 Position 조회
     */
    private fun getRequired(positionId: Long): Position =
        positionCommand.findById(positionId)
            ?: throw EntityNotFoundException("Position", positionId)
}
