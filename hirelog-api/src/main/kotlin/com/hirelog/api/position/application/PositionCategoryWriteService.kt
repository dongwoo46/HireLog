package com.hirelog.api.position.application

import com.hirelog.api.common.exception.EntityAlreadyExistsException
import com.hirelog.api.common.exception.EntityNotFoundException
import com.hirelog.api.position.application.port.PositionCategoryCommand
import com.hirelog.api.position.domain.PositionCategory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * PositionCategory Write Application Service
 *
 * 책임:
 * - PositionCategory 생성 및 상태 변경 유스케이스 실행
 * - 트랜잭션 경계 정의
 */
@Service
class PositionCategoryWriteService(
    private val positionCategoryCommand: PositionCategoryCommand
) {

    /**
     * PositionCategory 확보 (idempotent)
     *
     * 정책:
     * - normalizedName 기준 단일 PositionCategory 보장
     * - 존재하면 그대로 반환
     * - 없으면 신규 생성
     */
    @Transactional
    fun getOrCreate(name: String, description: String? = null): PositionCategory {
        val normalized = normalize(name)

        return positionCategoryCommand.findByNormalizedName(normalized)
            ?: try {
                positionCategoryCommand.save(PositionCategory.create(name, description))
            } catch (ex: DataIntegrityViolationException) {
                positionCategoryCommand.findByNormalizedName(normalized)
                    ?: throw ex
            }
    }

    /**
     * PositionCategory 신규 생성
     */
    @Transactional
    fun create(name: String, description: String?): PositionCategory {
        return try {
            positionCategoryCommand.save(PositionCategory.create(name, description))
        } catch (ex: DataIntegrityViolationException) {
            throw EntityAlreadyExistsException(
                "PositionCategory already exists. name=$name",
                ex
            )
        }
    }

    /**
     * PositionCategory 비활성화
     */
    @Transactional
    fun deactivate(categoryId: Long) {
        getRequired(categoryId).deactivate()
    }

    private fun getRequired(categoryId: Long): PositionCategory =
        positionCategoryCommand.findById(categoryId)
            ?: throw EntityNotFoundException("PositionCategory", categoryId)

    private fun normalize(value: String): String =
        value.lowercase()
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
}
