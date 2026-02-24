package com.hirelog.api.notification.domain

import com.hirelog.api.notification.domain.model.Notification
import com.hirelog.api.notification.domain.type.NotificationReferenceType
import com.hirelog.api.notification.domain.type.NotificationType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.*

@DisplayName("Notification 도메인 테스트")
class NotificationTest {

    private fun makeNotification(
        memberId: Long = 1L,
        title: String = "JD 요약 완료",
        type: NotificationType = NotificationType.JOB_SUMMARY_COMPLETED
    ): Notification = Notification.create(
        memberId = memberId,
        type = type,
        title = title,
        message = "백엔드 Engineer JD 요약이 완료되었습니다.",
        referenceType = NotificationReferenceType.JOB_SUMMARY,
        referenceId = 100L
    )

    @Nested
    @DisplayName("create 팩토리는")
    inner class CreateTest {

        @Test
        @DisplayName("정상 값으로 알림을 생성한다")
        fun shouldCreateNotification() {
            val notification = makeNotification()

            assertThat(notification.memberId).isEqualTo(1L)
            assertThat(notification.title).isEqualTo("JD 요약 완료")
            assertThat(notification.isRead).isFalse()
            assertThat(notification.readAt).isNull()
        }

        @Test
        @DisplayName("memberId가 0 이하이면 예외를 던진다")
        fun shouldThrowWhenInvalidMemberId() {
            assertThatThrownBy {
                Notification.create(
                    memberId = 0L,
                    type = NotificationType.JOB_SUMMARY_COMPLETED,
                    title = "알림"
                )
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("memberId must be positive")
        }

        @Test
        @DisplayName("blank title이면 예외를 던진다")
        fun shouldThrowWhenBlankTitle() {
            assertThatThrownBy {
                Notification.create(
                    memberId = 1L,
                    type = NotificationType.JOB_SUMMARY_COMPLETED,
                    title = "  "
                )
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("title must not be blank")
        }

        @Test
        @DisplayName("referenceType과 referenceId 없이도 생성된다")
        fun shouldCreateWithoutReference() {
            val notification = Notification.create(
                memberId = 1L,
                type = NotificationType.JOB_SUMMARY_COMPLETED,
                title = "시스템 알림"
            )

            assertThat(notification.referenceType).isNull()
            assertThat(notification.referenceId).isNull()
        }
    }

    @Nested
    @DisplayName("markAsRead는")
    inner class MarkAsReadTest {

        @Test
        @DisplayName("isRead=true로 변경하고 readAt을 설정한다")
        fun shouldMarkAsRead() {
            val notification = makeNotification()
            notification.markAsRead()

            assertThat(notification.isRead).isTrue()
            assertThat(notification.readAt).isNotNull()
        }

        @Test
        @DisplayName("이미 읽은 경우 재호출해도 readAt이 변경되지 않는다 (멱등)")
        fun shouldBeIdempotent() {
            val notification = makeNotification()
            notification.markAsRead()
            val firstReadAt = notification.readAt

            notification.markAsRead() // 두 번째 호출

            assertThat(notification.readAt).isEqualTo(firstReadAt)
        }
    }
}
