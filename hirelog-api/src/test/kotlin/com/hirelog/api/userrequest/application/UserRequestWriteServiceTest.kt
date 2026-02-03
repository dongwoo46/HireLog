package com.hirelog.api.userrequest.application

import com.hirelog.api.member.application.port.MemberQuery
import com.hirelog.api.userrequest.application.port.UserRequestCommand
import com.hirelog.api.userrequest.application.port.UserRequestQuery
import com.hirelog.api.userrequest.domain.UserRequest
import com.hirelog.api.userrequest.domain.UserRequestStatus
import com.hirelog.api.userrequest.domain.UserRequestType
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock

@ExtendWith(MockKExtension::class)
class UserRequestWriteServiceTest {

    @MockK
    lateinit var command: UserRequestCommand

    @MockK
    lateinit var query: UserRequestQuery

    @Mock
    lateinit var memberQuery: MemberQuery

    private lateinit var writeService: UserRequestWriteService

    @BeforeEach
    fun setUp() {
        writeService = UserRequestWriteService(command, query, memberQuery)
    }

    @Nested
    @DisplayName("create 테스트")
    inner class CreateTest {

        @Test
        @DisplayName("UserRequest 생성 성공")
        fun `should create UserRequest successfully`() {
            // given
            val memberId = 1L
            val requestType = UserRequestType.ERROR_REPORT
            val content = "오류 신고 내용"

            every { command.save(any()) } answers {
                firstArg()
            }

            // when
            val result = writeService.create(
                memberId = memberId,
                requestType = requestType,
                content = content
            )

            // then
            assertEquals(memberId, result.memberId)
            assertEquals(requestType, result.requestType)
            assertEquals(content, result.content)
            assertEquals(UserRequestStatus.OPEN, result.status)

            verify(exactly = 1) { command.save(any()) }
        }

        @Test
        @DisplayName("다양한 requestType으로 생성")
        fun `should create UserRequest with various types`() {
            // given
            every { command.save(any()) } answers { firstArg() }

            val types = UserRequestType.entries

            // when & then
            types.forEach { type ->
                val result = writeService.create(
                    memberId = 1L,
                    requestType = type,
                    content = "내용"
                )
                assertEquals(type, result.requestType)
            }

            verify(exactly = types.size) { command.save(any()) }
        }
    }

    @Nested
    @DisplayName("updateStatus 테스트")
    inner class UpdateStatusTest {

        @Test
        @DisplayName("상태 변경 성공 - RESOLVED")
        fun `should update status to RESOLVED successfully`() {
            // given
            val userRequest = UserRequest.create(
                memberId = 1L,
                requestType = UserRequestType.FEATURE_REQUEST,
                content = "기능 요청"
            )

            every { query.findById(1L) } returns userRequest
            every { command.save(any()) } answers { firstArg() }

            // when
            val result = writeService.updateStatus(1L, UserRequestStatus.RESOLVED)

            // then
            assertEquals(UserRequestStatus.RESOLVED, result.status)
            assertNotNull(result.resolvedAt)

            verify(exactly = 1) { query.findById(1L) }
            verify(exactly = 1) { command.save(any()) }
        }

        @Test
        @DisplayName("상태 변경 성공 - IN_PROGRESS")
        fun `should update status to IN_PROGRESS successfully`() {
            // given
            val userRequest = UserRequest.create(
                memberId = 1L,
                requestType = UserRequestType.MODIFY_REQUEST,
                content = "수정 요청"
            )

            every { query.findById(1L) } returns userRequest
            every { command.save(any()) } answers { firstArg() }

            // when
            val result = writeService.updateStatus(1L, UserRequestStatus.IN_PROGRESS)

            // then
            assertEquals(UserRequestStatus.IN_PROGRESS, result.status)
            assertNull(result.resolvedAt)
        }

        @Test
        @DisplayName("존재하지 않는 UserRequest 상태 변경 시 예외")
        fun `should throw exception when UserRequest not found`() {
            // given
            every { query.findById(999L) } returns null

            // when & then
            val exception = assertThrows<IllegalArgumentException> {
                writeService.updateStatus(999L, UserRequestStatus.RESOLVED)
            }

            assertEquals("UserRequest not found: 999", exception.message)
            verify(exactly = 1) { query.findById(999L) }
            verify(exactly = 0) { command.save(any()) }
        }

        @Test
        @DisplayName("REJECTED 상태로 변경 시 resolvedAt 설정")
        fun `should set resolvedAt when status changed to REJECTED`() {
            // given
            val userRequest = UserRequest.create(
                memberId = 1L,
                requestType = UserRequestType.REPROCESS_REQUEST,
                content = "재처리 요청"
            )

            every { query.findById(1L) } returns userRequest
            every { command.save(any()) } answers { firstArg() }

            // when
            val result = writeService.updateStatus(1L, UserRequestStatus.REJECTED)

            // then
            assertEquals(UserRequestStatus.REJECTED, result.status)
            assertNotNull(result.resolvedAt)
        }
    }
}
