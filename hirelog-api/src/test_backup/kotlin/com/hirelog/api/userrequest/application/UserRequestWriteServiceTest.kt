package com.hirelog.api.userrequest.application

import com.hirelog.api.member.application.port.MemberQuery
import com.hirelog.api.member.domain.MemberStatus
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

    @MockK
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
        @DisplayName("UserRequest 생성 성공 - ACTIVE 회원")
        fun create_success() {
            // given
            val memberId = 1L
            val requestType = UserRequestType.ERROR_REPORT
            val content = "오류 신고"

            every {
                memberQuery.existsByIdAndStatus(memberId, MemberStatus.ACTIVE)
            } returns true

            every { command.save(any()) } answers { firstArg() }

            // when
            val result = writeService.create(memberId, requestType, content)

            // then
            assertEquals(memberId, result.memberId)
            assertEquals(requestType, result.requestType)
            assertEquals(content, result.content)
            assertEquals(UserRequestStatus.OPEN, result.status)

            verify(exactly = 1) {
                memberQuery.existsByIdAndStatus(memberId, MemberStatus.ACTIVE)
            }
            verify(exactly = 1) { command.save(any()) }
        }

        @Test
        @DisplayName("UserRequest 생성 실패 - 존재하지 않는 회원")
        fun create_fail_member_not_exists() {
            // given
            val memberId = 999L

            every {
                memberQuery.existsByIdAndStatus(memberId, MemberStatus.ACTIVE)
            } returns false

            // when & then
            val exception = assertThrows<IllegalArgumentException> {
                writeService.create(
                    memberId = memberId,
                    requestType = UserRequestType.FEATURE_REQUEST,
                    content = "기능 요청"
                )
            }

            assertEquals(
                "존재하지 않는 사용자입니다. memberId=$memberId",
                exception.message
            )

            verify(exactly = 0) { command.save(any()) }
        }
    }

    @Nested
    @DisplayName("updateStatus 테스트")
    inner class UpdateStatusTest {

        @Test
        @DisplayName("상태 변경 성공")
        fun update_status_success() {
            // given
            val userRequest = UserRequest.create(
                memberId = 1L,
                requestType = UserRequestType.MODIFY_REQUEST,
                content = "수정 요청"
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
        @DisplayName("존재하지 않는 UserRequest 상태 변경 시 예외")
        fun update_status_fail_not_found() {
            // given
            every { query.findById(999L) } returns null

            // when & then
            val exception = assertThrows<IllegalArgumentException> {
                writeService.updateStatus(999L, UserRequestStatus.REJECTED)
            }

            assertEquals("UserRequest not found: 999", exception.message)
            verify(exactly = 0) { command.save(any()) }
        }
    }
}
