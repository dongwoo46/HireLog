package com.hirelog.api.userrequest.domain

import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("UserRequestComment")
class UserRequestCommentTest {

    private val userRequest: UserRequest = mockk(relaxed = true)

    @Nested
    @DisplayName("create()")
    inner class Create {

        @Test
        @DisplayName("모든 필드를 포함하여 UserRequestComment를 생성한다")
        fun `creates UserRequestComment with all fields`() {
            // when
            val comment = UserRequestComment.create(
                userRequest = userRequest,
                writerType = UserRequestCommentWriterType.USER,
                writerId = 1L,
                content = "문의 내용입니다"
            )

            // then
            assertThat(comment.userRequest).isEqualTo(userRequest)
            assertThat(comment.writerType).isEqualTo(UserRequestCommentWriterType.USER)
            assertThat(comment.writerId).isEqualTo(1L)
            assertThat(comment.content).isEqualTo("문의 내용입니다")
        }

        @Test
        @DisplayName("ADMIN writerType으로 생성할 수 있다")
        fun `creates comment with ADMIN writerType`() {
            // when
            val comment = UserRequestComment.create(
                userRequest = userRequest,
                writerType = UserRequestCommentWriterType.ADMIN,
                writerId = 10L,
                content = "답변입니다"
            )

            // then
            assertThat(comment.writerType).isEqualTo(UserRequestCommentWriterType.ADMIN)
            assertThat(comment.writerId).isEqualTo(10L)
        }

        @Test
        @DisplayName("생성 시 id 기본값은 0이다")
        fun `initial id is 0`() {
            // when
            val comment = UserRequestComment.create(
                userRequest = userRequest,
                writerType = UserRequestCommentWriterType.USER,
                writerId = 1L,
                content = "내용"
            )

            // then
            assertThat(comment.id).isEqualTo(0L)
        }

        @Test
        @DisplayName("동일한 userRequest에 여러 댓글을 각각 독립적으로 생성할 수 있다")
        fun `creates multiple independent comments for same userRequest`() {
            // when
            val comment1 = UserRequestComment.create(
                userRequest = userRequest,
                writerType = UserRequestCommentWriterType.USER,
                writerId = 1L,
                content = "첫 번째 댓글"
            )
            val comment2 = UserRequestComment.create(
                userRequest = userRequest,
                writerType = UserRequestCommentWriterType.ADMIN,
                writerId = 10L,
                content = "두 번째 댓글"
            )

            // then
            assertThat(comment1.content).isEqualTo("첫 번째 댓글")
            assertThat(comment2.content).isEqualTo("두 번째 댓글")
            assertThat(comment1).isNotSameAs(comment2)
        }
    }
}