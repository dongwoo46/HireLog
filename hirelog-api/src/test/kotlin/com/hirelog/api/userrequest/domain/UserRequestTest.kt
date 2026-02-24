package com.hirelog.api.userrequest.domain

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("UserRequest")
class UserRequestTest {

    private fun createOpenRequest(
        memberId: Long = 1L,
        title: String = "문의합니다",
        content: String = "내용입니다"
    ): UserRequest = UserRequest.create(
        memberId = memberId,
        title = title,
        requestType = UserRequestType.MODIFY_REQUEST,
        content = content
    )

    @Nested
    @DisplayName("create()")
    inner class Create {

        @Test
        @DisplayName("정상 입력으로 UserRequest를 생성한다")
        fun `creates UserRequest with valid inputs`() {
            // when
            val request = UserRequest.create(
                memberId = 1L,
                title = "문의합니다",
                requestType = UserRequestType.MODIFY_REQUEST,
                content = "내용입니다"
            )

            // then
            assertThat(request.memberId).isEqualTo(1L)
            assertThat(request.title).isEqualTo("문의합니다")
            assertThat(request.requestType).isEqualTo(UserRequestType.MODIFY_REQUEST)
            assertThat(request.content).isEqualTo("내용입니다")
        }

        @Test
        @DisplayName("생성 시 초기 상태는 OPEN이다")
        fun `initial status is OPEN`() {
            val request = createOpenRequest()
            assertThat(request.status()).isEqualTo(UserRequestStatus.OPEN)
        }

        @Test
        @DisplayName("생성 시 resolvedAt은 null이다")
        fun `initial resolvedAt is null`() {
            val request = createOpenRequest()
            assertThat(request.resolvedAt()).isNull()
        }

        @Test
        @DisplayName("title 앞뒤 공백은 trim된다")
        fun `trims whitespace from title`() {
            val request = createOpenRequest(title = "  문의합니다  ")
            assertThat(request.title).isEqualTo("문의합니다")
        }

        @Test
        @DisplayName("title이 blank면 IllegalArgumentException을 던진다")
        fun `throws IllegalArgumentException when title is blank`() {
            assertThatThrownBy {
                createOpenRequest(title = "   ")
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("title")
        }

        @Test
        @DisplayName("title이 200자를 초과하면 IllegalArgumentException을 던진다")
        fun `throws IllegalArgumentException when title exceeds 200 chars`() {
            assertThatThrownBy {
                createOpenRequest(title = "a".repeat(201))
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("200")
        }

        @Test
        @DisplayName("title이 정확히 200자면 생성에 성공한다")
        fun `creates successfully when title is exactly 200 chars`() {
            val request = createOpenRequest(title = "a".repeat(200))
            assertThat(request.title.length).isEqualTo(200)
        }

        @Test
        @DisplayName("content가 blank면 IllegalArgumentException을 던진다")
        fun `throws IllegalArgumentException when content is blank`() {
            assertThatThrownBy {
                createOpenRequest(content = "   ")
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("content")
        }
    }

    @Nested
    @DisplayName("updateStatus()")
    inner class UpdateStatus {

        @Test
        @DisplayName("OPEN → IN_PROGRESS 상태 변경이 가능하다")
        fun `can change from OPEN to IN_PROGRESS`() {
            val request = createOpenRequest()
            request.updateStatus(UserRequestStatus.IN_PROGRESS)
            assertThat(request.status()).isEqualTo(UserRequestStatus.IN_PROGRESS)
        }

        @Test
        @DisplayName("OPEN → RESOLVED 상태 변경 시 resolvedAt이 기록된다")
        fun `records resolvedAt when status changes to RESOLVED`() {
            val request = createOpenRequest()
            request.updateStatus(UserRequestStatus.RESOLVED)

            assertThat(request.status()).isEqualTo(UserRequestStatus.RESOLVED)
            assertThat(request.resolvedAt()).isNotNull()
        }

        @Test
        @DisplayName("OPEN → REJECTED 상태 변경 시 resolvedAt이 기록된다")
        fun `records resolvedAt when status changes to REJECTED`() {
            val request = createOpenRequest()
            request.updateStatus(UserRequestStatus.REJECTED)

            assertThat(request.status()).isEqualTo(UserRequestStatus.REJECTED)
            assertThat(request.resolvedAt()).isNotNull()
        }

        @Test
        @DisplayName("IN_PROGRESS → RESOLVED 상태 변경이 가능하다")
        fun `can change from IN_PROGRESS to RESOLVED`() {
            val request = createOpenRequest()
            request.updateStatus(UserRequestStatus.IN_PROGRESS)
            request.updateStatus(UserRequestStatus.RESOLVED)

            assertThat(request.status()).isEqualTo(UserRequestStatus.RESOLVED)
        }

        @Test
        @DisplayName("RESOLVED 상태에서는 어떤 상태로도 변경 불가하다")
        fun `cannot update status when already RESOLVED`() {
            val request = createOpenRequest()
            request.updateStatus(UserRequestStatus.RESOLVED)

            assertThatThrownBy {
                request.updateStatus(UserRequestStatus.OPEN)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("RESOLVED")
        }

        @Test
        @DisplayName("REJECTED 상태에서는 어떤 상태로도 변경 불가하다")
        fun `cannot update status when already REJECTED`() {
            val request = createOpenRequest()
            request.updateStatus(UserRequestStatus.REJECTED)

            assertThatThrownBy {
                request.updateStatus(UserRequestStatus.IN_PROGRESS)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("REJECTED")
        }

        @Test
        @DisplayName("RESOLVED → REJECTED 변경도 불가하다")
        fun `cannot change from RESOLVED to REJECTED`() {
            val request = createOpenRequest()
            request.updateStatus(UserRequestStatus.RESOLVED)

            assertThatThrownBy {
                request.updateStatus(UserRequestStatus.REJECTED)
            }.isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        @DisplayName("RESOLVED가 아닌 상태 변경 시 resolvedAt은 null로 유지된다")
        fun `resolvedAt remains null when not transitioning to terminal status`() {
            val request = createOpenRequest()
            request.updateStatus(UserRequestStatus.IN_PROGRESS)

            assertThat(request.resolvedAt()).isNull()
        }
    }

    @Nested
    @DisplayName("addComment()")
    inner class AddComment {

        @Test
        @DisplayName("OPEN 상태에서 댓글을 추가하면 comments에 포함된다")
        fun `adds comment to OPEN request`() {
            // given
            val request = createOpenRequest()

            // when
            val comment = request.addComment(
                writerType = UserRequestCommentWriterType.USER,
                writerId = 1L,
                content = "추가 문의입니다"
            )

            // then
            assertThat(request.getComments()).hasSize(1)
            assertThat(request.getComments()).contains(comment)
        }

        @Test
        @DisplayName("addComment()는 추가된 댓글 객체를 반환한다")
        fun `returns added comment`() {
            val request = createOpenRequest()

            val comment = request.addComment(
                writerType = UserRequestCommentWriterType.ADMIN,
                writerId = 10L,
                content = "답변입니다"
            )

            assertThat(comment).isNotNull()
        }

        @Test
        @DisplayName("OPEN이 아닌 상태에서 댓글 추가 시 IllegalArgumentException을 던진다")
        fun `throws IllegalArgumentException when request is not OPEN`() {
            val request = createOpenRequest()
            request.updateStatus(UserRequestStatus.RESOLVED)

            assertThatThrownBy {
                request.addComment(
                    writerType = UserRequestCommentWriterType.USER,
                    writerId = 1L,
                    content = "추가 문의입니다"
                )
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("closed")
        }

        @Test
        @DisplayName("IN_PROGRESS 상태에서도 댓글 추가 시 IllegalArgumentException을 던진다")
        fun `throws IllegalArgumentException when status is IN_PROGRESS`() {
            val request = createOpenRequest()
            request.updateStatus(UserRequestStatus.IN_PROGRESS)

            assertThatThrownBy {
                request.addComment(
                    writerType = UserRequestCommentWriterType.ADMIN,
                    writerId = 10L,
                    content = "답변입니다"
                )
            }.isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        @DisplayName("여러 댓글을 순서대로 추가할 수 있다")
        fun `can add multiple comments in order`() {
            val request = createOpenRequest()

            request.addComment(UserRequestCommentWriterType.USER, 1L, "첫 번째 댓글")
            request.addComment(UserRequestCommentWriterType.ADMIN, 10L, "두 번째 댓글")
            request.addComment(UserRequestCommentWriterType.USER, 1L, "세 번째 댓글")

            assertThat(request.getComments()).hasSize(3)
        }
    }

    @Nested
    @DisplayName("getComments()")
    inner class GetComments {

        @Test
        @DisplayName("댓글이 없으면 빈 리스트를 반환한다")
        fun `returns empty list when no comments`() {
            val request = createOpenRequest()
            assertThat(request.getComments()).isEmpty()
        }

        @Test
        @DisplayName("반환된 리스트는 불변이다 (외부에서 수정 불가)")
        fun `returned list is immutable`() {
            val request = createOpenRequest()
            request.addComment(UserRequestCommentWriterType.USER, 1L, "테스트")

            assertThatThrownBy {
                (request.getComments() as MutableList<*>).clear()
            }.isInstanceOf(UnsupportedOperationException::class.java)
        }
    }
}