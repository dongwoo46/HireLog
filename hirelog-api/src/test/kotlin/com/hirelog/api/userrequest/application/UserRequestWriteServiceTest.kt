package com.hirelog.api.userrequest.application

import com.hirelog.api.member.application.port.MemberQuery
import com.hirelog.api.member.domain.MemberRole
import com.hirelog.api.member.domain.MemberStatus
import com.hirelog.api.notification.application.NotificationWriteService
import com.hirelog.api.notification.domain.type.NotificationReferenceType
import com.hirelog.api.notification.domain.type.NotificationType
import com.hirelog.api.userrequest.application.port.UserRequestCommand
import com.hirelog.api.userrequest.application.port.UserRequestQuery
import com.hirelog.api.userrequest.domain.UserRequest
import com.hirelog.api.userrequest.domain.UserRequestCommentWriterType
import com.hirelog.api.userrequest.domain.UserRequestStatus
import com.hirelog.api.userrequest.domain.UserRequestType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("UserRequestWriteService")
class UserRequestWriteServiceTest {

    private lateinit var command: UserRequestCommand
    private lateinit var query: UserRequestQuery
    private lateinit var memberQuery: MemberQuery
    private lateinit var notificationWriteService: NotificationWriteService
    private lateinit var userRequestWriteService: UserRequestWriteService

    @BeforeEach
    fun setUp() {
        command = mockk()
        query = mockk()
        memberQuery = mockk()
        notificationWriteService = mockk()
        userRequestWriteService = UserRequestWriteService(
            command, query, memberQuery, notificationWriteService
        )
    }

    @Nested
    @DisplayName("create()")
    inner class Create {

        @Test
        @DisplayName("ACTIVE 회원이면 UserRequest를 생성하고 저장된 결과를 반환한다")
        fun `creates UserRequest when member is ACTIVE`() {
            // given
            val memberId = 1L
            val saved = mockk<UserRequest>()

            every { memberQuery.existsByIdAndStatus(memberId, MemberStatus.ACTIVE) } returns true
            every { command.save(any()) } returns saved

            // when
            val result = userRequestWriteService.create(
                memberId = memberId,
                requestType = UserRequestType.MODIFY_REQUEST,
                title = "문의합니다",
                content = "내용입니다"
            )

            // then
            assertThat(result).isEqualTo(saved)
            verify(exactly = 1) { command.save(any()) }
        }

        @Test
        @DisplayName("ACTIVE 회원이 아니면 IllegalArgumentException을 던진다")
        fun `throws IllegalArgumentException when member is not ACTIVE`() {
            // given
            val memberId = 999L

            every { memberQuery.existsByIdAndStatus(memberId, MemberStatus.ACTIVE) } returns false

            // when & then
            assertThatThrownBy {
                userRequestWriteService.create(
                    memberId = memberId,
                    requestType = UserRequestType.MODIFY_REQUEST,
                    title = "문의합니다",
                    content = "내용입니다"
                )
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining(memberId.toString())

            verify(exactly = 0) { command.save(any()) }
        }
    }

    @Nested
    @DisplayName("addComment()")
    inner class AddComment {

        @Test
        @DisplayName("ADMIN 역할이면 writerType이 ADMIN으로 설정된다")
        fun `sets writerType ADMIN when memberRole is ADMIN`() {
            // given
            val memberId = 10L
            val userRequestId = 1L
            val commentId = 100L

            val userRequest = mockk<UserRequest>(relaxed = true) {
                every { this@mockk.memberId } returns 99L // 요청자는 다른 사람
                every { id } returns userRequestId
                every { title } returns "제목"
                every { addComment(UserRequestCommentWriterType.ADMIN, memberId, any()) } returns mockk {
                    every { id } returns commentId
                }
            }

            every { query.findById(userRequestId) } returns userRequest
            every { command.save(userRequest) } returns userRequest
            every { notificationWriteService.create(any(), any(), any(), any(), any(), any()) } returns mockk()

            // when
            val result = userRequestWriteService.addComment(
                memberId = memberId,
                memberRole = MemberRole.ADMIN,
                userRequestId = userRequestId,
                content = "답변입니다"
            )

            // then
            assertThat(result).isEqualTo(commentId)
            verify(exactly = 1) {
                userRequest.addComment(UserRequestCommentWriterType.ADMIN, memberId, "답변입니다")
            }
        }

        @Test
        @DisplayName("USER 역할이면 writerType이 USER로 설정된다")
        fun `sets writerType USER when memberRole is USER`() {
            // given
            val memberId = 20L
            val userRequestId = 2L
            val commentId = 200L

            val userRequest = mockk<UserRequest>(relaxed = true) {
                every { this@mockk.memberId } returns 99L
                every { id } returns userRequestId
                every { title } returns "제목"
                every { addComment(UserRequestCommentWriterType.USER, memberId, any()) } returns mockk {
                    every { id } returns commentId
                }
            }

            every { query.findById(userRequestId) } returns userRequest
            every { command.save(userRequest) } returns userRequest
            every { notificationWriteService.create(any(), any(), any(), any(), any(), any()) } returns mockk()

            // when
            val result = userRequestWriteService.addComment(
                memberId = memberId,
                memberRole = MemberRole.USER,
                userRequestId = userRequestId,
                content = "추가 문의입니다"
            )

            // then
            assertThat(result).isEqualTo(commentId)
            verify(exactly = 1) {
                userRequest.addComment(UserRequestCommentWriterType.USER, memberId, "추가 문의입니다")
            }
        }

        @Test
        @DisplayName("댓글 작성자와 요청자가 다르면 알림을 전송한다")
        fun `sends notification when commenter is not the requester`() {
            // given
            val commenterId = 10L
            val requesterId = 99L
            val userRequestId = 1L

            val userRequest = mockk<UserRequest>(relaxed = true) {
                every { this@mockk.memberId } returns requesterId
                every { id } returns userRequestId
                every { title } returns "제목"
                every { addComment(any(), any(), any()) } returns mockk { every { id } returns 1L }
            }

            every { query.findById(userRequestId) } returns userRequest
            every { command.save(userRequest) } returns userRequest
            every { notificationWriteService.create(any(), any(), any(), any(), any(), any()) } returns mockk()

            // when
            userRequestWriteService.addComment(
                memberId = commenterId,
                memberRole = MemberRole.ADMIN,
                userRequestId = userRequestId,
                content = "답변입니다"
            )

            // then
            verify(exactly = 1) {
                notificationWriteService.create(
                    memberId = requesterId,
                    type = NotificationType.USER_REQUEST_REPLIED,
                    title = "내 요청에 답변이 등록되었습니다",
                    message = "제목",
                    referenceType = NotificationReferenceType.USER_REQUEST,
                    referenceId = userRequestId
                )
            }
        }

        @Test
        @DisplayName("댓글 작성자와 요청자가 같으면 알림을 전송하지 않는다")
        fun `does not send notification when commenter is the requester`() {
            // given
            val memberId = 10L  // 요청자 = 댓글 작성자 동일
            val userRequestId = 1L

            val userRequest = mockk<UserRequest>(relaxed = true) {
                every { this@mockk.memberId } returns memberId  // 본인 요청
                every { id } returns userRequestId
                every { title } returns "제목"
                every { addComment(any(), any(), any()) } returns mockk { every { id } returns 1L }
            }

            every { query.findById(userRequestId) } returns userRequest
            every { command.save(userRequest) } returns userRequest

            // when
            userRequestWriteService.addComment(
                memberId = memberId,
                memberRole = MemberRole.USER,
                userRequestId = userRequestId,
                content = "내용 추가합니다"
            )

            // then
            verify(exactly = 0) { notificationWriteService.create(any(), any(), any(), any(), any(), any()) }
        }

        @Test
        @DisplayName("알림 전송 실패 시 예외를 삼키고 commentId를 정상 반환한다")
        fun `swallows notification exception and returns commentId`() {
            // given
            val commenterId = 10L
            val requesterId = 99L
            val userRequestId = 1L
            val commentId = 100L

            val userRequest = mockk<UserRequest>(relaxed = true) {
                every { this@mockk.memberId } returns requesterId
                every { id } returns userRequestId
                every { title } returns "제목"
                every { addComment(any(), any(), any()) } returns mockk { every { id } returns commentId }
            }

            every { query.findById(userRequestId) } returns userRequest
            every { command.save(userRequest) } returns userRequest
            every {
                notificationWriteService.create(any(), any(), any(), any(), any(), any())
            } throws RuntimeException("알림 서버 오류")

            // when
            val result = userRequestWriteService.addComment(
                memberId = commenterId,
                memberRole = MemberRole.ADMIN,
                userRequestId = userRequestId,
                content = "답변입니다"
            )

            // then - 알림 실패해도 commentId 정상 반환
            assertThat(result).isEqualTo(commentId)
        }

        @Test
        @DisplayName("UserRequest가 존재하지 않으면 IllegalArgumentException을 던진다")
        fun `throws IllegalArgumentException when UserRequest not found`() {
            // given
            every { query.findById(any()) } returns null

            // when & then
            assertThatThrownBy {
                userRequestWriteService.addComment(
                    memberId = 1L,
                    memberRole = MemberRole.USER,
                    userRequestId = 999L,
                    content = "내용"
                )
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("999")
        }
    }

    @Nested
    @DisplayName("updateStatus()")
    inner class UpdateStatus {

        @Test
        @DisplayName("ADMIN이면 상태를 변경하고 저장된 결과를 반환한다")
        fun `updates status and returns saved result when ADMIN`() {
            // given
            val userRequestId = 1L
            val userRequest = mockk<UserRequest>(relaxed = true)
            val saved = mockk<UserRequest>()

            every { query.findById(userRequestId) } returns userRequest
            every { command.save(userRequest) } returns saved

            // when
            val result = userRequestWriteService.updateStatus(
                memberRole = MemberRole.ADMIN,
                userRequestId = userRequestId,
                status = UserRequestStatus.IN_PROGRESS
            )

            // then
            assertThat(result).isEqualTo(saved)
            verify(exactly = 1) { userRequest.updateStatus(UserRequestStatus.IN_PROGRESS) }
            verify(exactly = 1) { command.save(userRequest) }
        }

        @Test
        @DisplayName("ADMIN이 아니면 IllegalArgumentException을 던진다")
        fun `throws IllegalArgumentException when not ADMIN`() {
            // when & then
            assertThatThrownBy {
                userRequestWriteService.updateStatus(
                    memberRole = MemberRole.USER,
                    userRequestId = 1L,
                    status = UserRequestStatus.IN_PROGRESS
                )
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("ADMIN")

            verify(exactly = 0) { query.findById(any()) }
            verify(exactly = 0) { command.save(any()) }
        }

        @Test
        @DisplayName("UserRequest가 존재하지 않으면 IllegalArgumentException을 던진다")
        fun `throws IllegalArgumentException when UserRequest not found`() {
            // given
            every { query.findById(any()) } returns null

            // when & then
            assertThatThrownBy {
                userRequestWriteService.updateStatus(
                    memberRole = MemberRole.ADMIN,
                    userRequestId = 999L,
                    status = UserRequestStatus.RESOLVED
                )
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("999")
        }
    }
}