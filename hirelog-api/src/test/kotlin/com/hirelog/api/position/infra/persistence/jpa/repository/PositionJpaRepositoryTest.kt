package com.hirelog.api.position.infra.persistence.jpa.repository

import com.hirelog.api.position.domain.Position
import com.hirelog.api.position.domain.PositionCategory
import com.hirelog.api.position.domain.PositionStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager

@DataJpaTest
@DisplayName("PositionJpaRepository 테스트")
class PositionJpaRepositoryTest @Autowired constructor(
    private val positionRepository: PositionJpaRepository,
    private val em: TestEntityManager
) {

    private fun createCategory(): PositionCategory {
        val category = PositionCategory.create(
            name = "Engineering",
            description = "엔지니어링 직군"
        )
        em.persist(category)
        return category
    }

    @Test
    @DisplayName("save & findByNormalizedName: 저장 후 정규화된 이름으로 조회 가능해야 한다")
    fun save_and_find() {
        // given
        val category = createCategory()
        val position = Position.create(
            name = "Backend Developer",
            description = "Server side",
            positionCategory = category
        )
        positionRepository.save(position)

        // when
        val found = positionRepository.findByNormalizedName("backend_developer")

        // then
        assertNotNull(found)
        assertEquals("Backend Developer", found?.name)
        assertEquals("backend_developer", found?.normalizedName)
        assertEquals(PositionStatus.CANDIDATE, found?.status)
        assertEquals(category.id, found?.category?.id)
    }

    @Test
    @DisplayName("findAllByStatus: 특정 상태인 포지션만 조회되어야 한다")
    fun find_all_by_status() {
        // given
        val category = createCategory()

        val active1 = Position.create("Active1", null, category).apply { activate() }
        val active2 = Position.create("Active2", null, category).apply { activate() }
        val candidate = Position.create("Candidate", null, category)

        positionRepository.saveAll(listOf(active1, active2, candidate))

        // when
        val activePositions = positionRepository.findAllByStatus(PositionStatus.ACTIVE)
        val candidatePositions = positionRepository.findAllByStatus(PositionStatus.CANDIDATE)

        // then
        assertEquals(2, activePositions.size)
        assertEquals(1, candidatePositions.size)
    }
}
