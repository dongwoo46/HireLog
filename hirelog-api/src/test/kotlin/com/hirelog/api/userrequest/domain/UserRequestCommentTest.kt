package com.hirelog.api.userrequest.domain

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class UserRequestCommentTest {

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
        @DisplayName("USER 타입 댓글 생성")
        fun `should create comment with USER writer type`() {
            // given
            val userRequest = createTestUserRequest()

            // when
            val comment = UserRequestComment.create(
                userRequest = userRequest,
                writerType = UserRequestCommentWriterType.USER,
                writerId = 100L,
                content = "사용자 댓글입니다."
            )

            // then
            assertEquals(userRequest, comment.userRequest)
            assertEquals(UserRequestCommentWriterType.USER, comment.writerType)
            assertEquals(100L, comment.writerId)
            assertEquals("사용자 댓글입니다.", comment.content)
        }

        @Test
        @DisplayName("ADMIN 타입 댓글 생성")
        fun `should create comment with ADMIN writer type`() {
            // given
            val userRequest = createTestUserRequest()

            // when
            val comment = UserRequestComment.create(
                userRequest = userRequest,
                writerType = UserRequestCommentWriterType.ADMIN,
                writerId = 1L,
                content = "관리자 답변입니다."
            )

            // then
            assertEquals(userRequest, comment.userRequest)
            assertEquals(UserRequestCommentWriterType.ADMIN, comment.writerType)
            assertEquals(1L, comment.writerId)
            assertEquals("관리자 답변입니다.", comment.content)
        }

        @Test
        @DisplayName("모든 WriterType으로 생성 가능")
        fun `should create comment with all writer types`() {
            // given
            val userRequest = createTestUserRequest()
            val writerTypes = UserRequestCommentWriterType.entries

            // when & then
            writerTypes.forEach { writerType ->
                val comment = UserRequestComment.create(
                    userRequest = userRequest,
                    writerType = writerType,
                    writerId = 1L,
                    content = "댓글"
                )
                assertEquals(writerType, comment.writerType)
            }
        }
    }
}
