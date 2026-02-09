package com.hirelog.api.userrequest.application

import com.hirelog.api.userrequest.application.port.UserRequestCommentCommand
import com.hirelog.api.userrequest.application.port.UserRequestQuery
import com.hirelog.api.userrequest.domain.UserRequest
import com.hirelog.api.userrequest.domain.UserRequestComment
import com.hirelog.api.userrequest.domain.UserRequestCommentWriterType
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

@ExtendWith(MockKExtension::class)
class UserRequestCommentWriteServiceTest {

    @MockK
    lateinit var command: UserRequestCommentCommand

    @MockK
    lateinit var userRequestQuery: UserRequestQuery

    private lateinit var writeService: UserRequestCommentWriteService

    @BeforeEach
    fun setUp() {
        writeService = UserRequestCommentWriteService(command, userRequestQuery)
    }

    private fun createTestUserRequest(): UserRequest =
        UserRequest.create(
            memberId = 1L,
            requestType = UserRequestType.ERROR_REPORT,
            content = "테스트 요청"
        )

    @Nested
    @DisplayName("create 테스트")
    inner class CreateTest {

        @Test
        @DisplayName("USER 타입 댓글 생성 성공")
        fun `should create comment with USER writer type`() {
            // given
            val userRequest = createTestUserRequest()
            val userRequestId = 1L
            val writerId = 100L
            val content = "사용자 댓글입니다."

            every { userRequestQuery.findById(userRequestId) } returns userRequest
            every { command.save(any()) } answers { firstArg() }

            // when
            val result = writeService.create(
                userRequestId = userRequestId,
                writerType = UserRequestCommentWriterType.USER,
                writerId = writerId,
                content = content
            )

            // then
            assertEquals(userRequest, result.userRequest)
            assertEquals(UserRequestCommentWriterType.USER, result.writerType)
            assertEquals(writerId, result.writerId)
            assertEquals(content, result.content)

            verify(exactly = 1) { userRequestQuery.findById(userRequestId) }
            verify(exactly = 1) { command.save(any()) }
        }

        @Test
        @DisplayName("ADMIN 타입 댓글 생성 성공")
        fun `should create comment with ADMIN writer type`() {
            // given
            val userRequest = createTestUserRequest()
            val userRequestId = 1L
            val adminId = 1L
            val content = "관리자 답변입니다."

            every { userRequestQuery.findById(userRequestId) } returns userRequest
            every { command.save(any()) } answers { firstArg() }

            // when
            val result = writeService.create(
                userRequestId = userRequestId,
                writerType = UserRequestCommentWriterType.ADMIN,
                writerId = adminId,
                content = content
            )

            // then
            assertEquals(UserRequestCommentWriterType.ADMIN, result.writerType)
            assertEquals(adminId, result.writerId)
            assertEquals(content, result.content)
        }

        @Test
        @DisplayName("존재하지 않는 UserRequest에 댓글 작성 시 예외")
        fun `should throw exception when UserRequest not found`() {
            // given
            val nonExistentId = 999L

            every { userRequestQuery.findById(nonExistentId) } returns null

            // when & then
            val exception = assertThrows<IllegalArgumentException> {
                writeService.create(
                    userRequestId = nonExistentId,
                    writerType = UserRequestCommentWriterType.USER,
                    writerId = 1L,
                    content = "댓글"
                )
            }

            assertEquals("UserRequest not found: $nonExistentId", exception.message)
            verify(exactly = 1) { userRequestQuery.findById(nonExistentId) }
            verify(exactly = 0) { command.save(any()) }
        }

        @Test
        @DisplayName("모든 WriterType으로 댓글 생성 가능")
        fun `should create comment with all writer types`() {
            // given
            val userRequest = createTestUserRequest()
            val writerTypes = UserRequestCommentWriterType.entries

            every { userRequestQuery.findById(any()) } returns userRequest
            every { command.save(any()) } answers { firstArg() }

            // when & then
            writerTypes.forEach { writerType ->
                val result = writeService.create(
                    userRequestId = 1L,
                    writerType = writerType,
                    writerId = 1L,
                    content = "댓글"
                )
                assertEquals(writerType, result.writerType)
            }

            verify(exactly = writerTypes.size) { userRequestQuery.findById(any()) }
            verify(exactly = writerTypes.size) { command.save(any()) }
        }
    }
}
