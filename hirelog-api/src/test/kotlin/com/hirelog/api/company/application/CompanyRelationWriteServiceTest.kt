package com.hirelog.api.company.application

import com.hirelog.api.common.exception.EntityAlreadyExistsException
import com.hirelog.api.company.application.port.CompanyRelationCommand
import com.hirelog.api.company.application.port.CompanyRelationQuery
import com.hirelog.api.company.application.view.CompanyRelationView
import com.hirelog.api.company.domain.CompanyRelation
import com.hirelog.api.company.domain.CompanyRelationType
import io.mockk.*
import io.mockk.impl.annotations.MockK
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.dao.DataIntegrityViolationException

@ExtendWith(io.mockk.junit5.MockKExtension::class)
@DisplayName("CompanyRelationWriteService 테스트")
class CompanyRelationWriteServiceTest {

    @MockK
    lateinit var command: CompanyRelationCommand

    @MockK
    lateinit var query: CompanyRelationQuery

    private lateinit var service: CompanyRelationWriteService

    @BeforeEach
    fun setUp() {
        service = CompanyRelationWriteService(command, query)
    }

    @Nested
    @DisplayName("create 테스트")
    inner class CreateTest {

        @Test
        @DisplayName("성공적으로 관계를 생성하고 저장한다")
        fun create_success() {
            // given
            val slot = slot<CompanyRelation>()
            every { command.save(capture(slot)) } returns Unit

            // when
            val result = service.create(
                parentCompanyId = 1L,
                childCompanyId = 2L,
                relationType = CompanyRelationType.SUBSIDIARY
            )

            // then - 반환값 검증
            assertEquals(1L, result.parentCompanyId)
            assertEquals(2L, result.childCompanyId)
            assertEquals(CompanyRelationType.SUBSIDIARY, result.relationType)

            // then - 저장된 엔티티 검증 (🔥 핵심)
            val saved = slot.captured
            assertEquals(1L, saved.parentCompanyId)
            assertEquals(2L, saved.childCompanyId)
            assertEquals(CompanyRelationType.SUBSIDIARY, saved.relationType)

            verify(exactly = 1) { command.save(any()) }
        }


        @Test
        @DisplayName("중복 생성 시 EntityAlreadyExistsException으로 변환된다")
        fun create_fail_when_duplicate() {
            // given
            every { command.save(any()) } throws DataIntegrityViolationException("duplicate key")

            // when
            val exception = assertThrows<EntityAlreadyExistsException> {
                service.create(1L, 2L, CompanyRelationType.SUBSIDIARY)
            }

            // then
            assertEquals(
                "CompanyRelation already exists. identifier=parent=1, child=2",
                exception.message
            )

            verify(exactly = 1) { command.save(any()) }
        }
    }

    @Nested
    @DisplayName("delete 테스트")
    inner class DeleteTest {

        @Test
        @DisplayName("관계 View와 Entity가 존재하면 삭제한다")
        fun delete_success() {
            // given
            val view = mockk<CompanyRelationView>()
            val relation = mockk<CompanyRelation>()

            every { view.id } returns 10L
            every { query.findView(1L, 2L) } returns view
            every { command.findById(10L) } returns relation
            every { command.delete(relation) } just Runs

            // when
            service.delete(1L, 2L)

            // then
            verify(exactly = 1) { query.findView(1L, 2L) }
            verify(exactly = 1) { command.findById(10L) }
            verify(exactly = 1) { command.delete(relation) }
        }

        @Test
        @DisplayName("관계 View가 없으면 no-op 한다")
        fun delete_no_op_when_view_not_found() {
            // given
            every { query.findView(any(), any()) } returns null

            // when
            service.delete(1L, 2L)

            // then
            verify(exactly = 1) { query.findView(1L, 2L) }
            verify(exactly = 0) { command.findById(any()) }
            verify(exactly = 0) { command.delete(any()) }
        }

        @Test
        @DisplayName("Entity가 없으면 no-op 한다 (동시성 삭제 등)")
        fun delete_no_op_when_entity_not_found() {
            // given
            val view = mockk<CompanyRelationView>()
            every { view.id } returns 10L
            every { query.findView(1L, 2L) } returns view
            every { command.findById(10L) } returns null

            // when
            service.delete(1L, 2L)

            // then
            verify(exactly = 1) { query.findView(1L, 2L) }
            verify(exactly = 1) { command.findById(10L) }
            verify(exactly = 0) { command.delete(any()) }
        }
    }
}
