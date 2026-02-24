package com.hirelog.api.notification.application

import com.hirelog.api.notification.application.port.NotificationCommand
import com.hirelog.api.notification.domain.model.Notification
import com.hirelog.api.notification.domain.type.NotificationReferenceType
import com.hirelog.api.notification.domain.type.NotificationType
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("NotificationWriteService")
class NotificationWriteServiceTest {

    private lateinit var notificationCommand: NotificationCommand
    private lateinit var notificationWriteService: NotificationWriteService

    @BeforeEach
    fun setUp() {
        notificationCommand = mockk()
        notificationWriteService = NotificationWriteService(notificationCommand)
    }

    @Nested
    @DisplayName("create()")
    inner class Create {

        @Test
        @DisplayName("필수 파라미터만으로 알림을 생성하고 저장한다")
        fun `creates notification with required params only`() {
            // given
            val memberId = 1L
            val type = NotificationType.JOB_SUMMARY_COMPLETED
            val title = "새로운 공고 요약"

            val savedNotification = mockk<Notification>()
            every { notificationCommand.save(any()) } returns savedNotification

            // when
            val result = notificationWriteService.create(
                memberId = memberId,
                type = type,
                title = title
            )

            // then
            assertThat(result).isEqualTo(savedNotification)
            verify(exactly = 1) { notificationCommand.save(any()) }
        }

        @Test
        @DisplayName("모든 파라미터를 포함하여 알림을 생성하고 저장한다")
        fun `creates notification with all params`() {
            // given
            val memberId = 2L
            val type = NotificationType.JOB_SUMMARY_COMPLETED
            val title = "공고 요약 완료"
            val message = "요약이 완료되었습니다"
            val referenceType = NotificationReferenceType.JOB_SUMMARY
            val referenceId = 100L
            val metadata = mapOf("key" to "value", "count" to 3)

            val notificationSlot = slot<Notification>()
            val savedNotification = mockk<Notification>()
            every { notificationCommand.save(capture(notificationSlot)) } returns savedNotification

            // when
            val result = notificationWriteService.create(
                memberId = memberId,
                type = type,
                title = title,
                message = message,
                referenceType = referenceType,
                referenceId = referenceId,
                metadata = metadata
            )

            // then
            assertThat(result).isEqualTo(savedNotification)
            verify(exactly = 1) { notificationCommand.save(any()) }
        }

        @Test
        @DisplayName("저장된 알림 객체를 그대로 반환한다")
        fun `returns saved notification`() {
            // given
            val savedNotification = mockk<Notification>()
            every { notificationCommand.save(any()) } returns savedNotification

            // when
            val result = notificationWriteService.create(
                memberId = 1L,
                type = NotificationType.JOB_SUMMARY_COMPLETED,
                title = "test"
            )

            // then
            assertThat(result).isSameAs(savedNotification)
        }
    }

    @Nested
    @DisplayName("markAsRead()")
    inner class MarkAsRead {

        @Test
        @DisplayName("알림 목록을 읽음 처리한다")
        fun `marks notifications as read`() {
            // given
            val memberId = 1L
            val notificationIds = listOf(1L, 2L, 3L)

            val notification1 = mockk<Notification>(relaxed = true)
            val notification2 = mockk<Notification>(relaxed = true)
            val notification3 = mockk<Notification>(relaxed = true)

            every {
                notificationCommand.findByIdsAndMemberId(notificationIds, memberId)
            } returns listOf(notification1, notification2, notification3)

            // when
            notificationWriteService.markAsRead(notificationIds, memberId)

            // then
            verify(exactly = 1) { notification1.markAsRead() }
            verify(exactly = 1) { notification2.markAsRead() }
            verify(exactly = 1) { notification3.markAsRead() }
        }

        @Test
        @DisplayName("조회된 알림이 없으면 아무 작업도 하지 않는다")
        fun `does nothing when no notifications found`() {
            // given
            val memberId = 1L
            val notificationIds = listOf(999L)

            every {
                notificationCommand.findByIdsAndMemberId(notificationIds, memberId)
            } returns emptyList()

            // when & then (예외 없이 정상 종료)
            notificationWriteService.markAsRead(notificationIds, memberId)

            verify(exactly = 0) { notificationCommand.save(any()) }
        }

        @Test
        @DisplayName("본인 소유의 알림만 읽음 처리된다 - 일부만 조회된 경우")
        fun `only marks owned notifications as read`() {
            // given
            val memberId = 1L
            val notificationIds = listOf(1L, 2L, 3L)

            // 3개 요청했지만 본인 소유는 2개만 조회됨
            val notification1 = mockk<Notification>(relaxed = true)
            val notification2 = mockk<Notification>(relaxed = true)

            every {
                notificationCommand.findByIdsAndMemberId(notificationIds, memberId)
            } returns listOf(notification1, notification2)

            // when
            notificationWriteService.markAsRead(notificationIds, memberId)

            // then
            verify(exactly = 1) { notification1.markAsRead() }
            verify(exactly = 1) { notification2.markAsRead() }
        }

        @Test
        @DisplayName("빈 ID 목록으로 요청하면 아무 작업도 하지 않는다")
        fun `does nothing with empty id list`() {
            // given
            val memberId = 1L

            every {
                notificationCommand.findByIdsAndMemberId(emptyList(), memberId)
            } returns emptyList()

            // when
            notificationWriteService.markAsRead(emptyList(), memberId)

            // then
            verify(exactly = 1) { notificationCommand.findByIdsAndMemberId(emptyList(), memberId) }
        }
    }
}