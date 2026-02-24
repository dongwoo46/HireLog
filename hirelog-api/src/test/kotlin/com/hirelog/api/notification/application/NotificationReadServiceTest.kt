package com.hirelog.api.notification.application

import com.hirelog.api.common.application.port.PagedResult
import com.hirelog.api.notification.application.port.NotificationQuery
import com.hirelog.api.notification.application.view.NotificationView
import com.hirelog.api.notification.domain.type.NotificationType
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import java.time.LocalDateTime

@DisplayName("NotificationReadService 테스트")
class NotificationReadServiceTest {

    private lateinit var service: NotificationReadService
    private lateinit var notificationQuery: NotificationQuery

    @BeforeEach
    fun setUp() {
        notificationQuery = mockk()
        service = NotificationReadService(notificationQuery)
    }

    private fun makeView(id: Long, isRead: Boolean = false) = NotificationView(
        id = id,
        type = NotificationType.JOB_SUMMARY_COMPLETED,
        title = "알림 $id",
        message = null,
        referenceType = null,
        referenceId = null,
        metadata = emptyMap(),
        isRead = isRead,
        readAt = null,
        createdAt = LocalDateTime.now()
    )

    @Nested
    @DisplayName("getNotifications는")
    inner class GetNotificationsTest {

        @Test
        @DisplayName("PagedResult를 올바르게 조합하여 반환한다")
        fun shouldReturnPagedResult() {
            val views = listOf(makeView(1L), makeView(2L))
            every { notificationQuery.findByMemberId(1L, null, 0, 10) } returns views
            every { notificationQuery.countByMemberId(1L, null) } returns 2L

            val result = service.getNotifications(memberId = 1L, isRead = null, page = 0, size = 10)

            assertThat(result.items).hasSize(2)
            assertThat(result.totalElements).isEqualTo(2L)
            assertThat(result.page).isEqualTo(0)
            assertThat(result.size).isEqualTo(10)
        }

        @Test
        @DisplayName("isRead 필터를 쿼리에 그대로 전달한다")
        fun shouldPassIsReadFilter() {
            every { notificationQuery.findByMemberId(1L, false, 0, 5) } returns emptyList()
            every { notificationQuery.countByMemberId(1L, false) } returns 0L

            service.getNotifications(memberId = 1L, isRead = false, page = 0, size = 5)

            verify { notificationQuery.findByMemberId(1L, false, 0, 5) }
            verify { notificationQuery.countByMemberId(1L, false) }
        }

        @Test
        @DisplayName("결과가 없으면 빈 PagedResult를 반환한다")
        fun shouldReturnEmptyResult() {
            every { notificationQuery.findByMemberId(any(), any(), any(), any()) } returns emptyList()
            every { notificationQuery.countByMemberId(any(), any()) } returns 0L

            val result = service.getNotifications(memberId = 1L, isRead = null, page = 0, size = 10)

            assertThat(result.items).isEmpty()
            assertThat(result.totalElements).isEqualTo(0L)
        }
    }

    @Nested
    @DisplayName("getUnreadCount는")
    inner class GetUnreadCountTest {

        @Test
        @DisplayName("쿼리에서 반환한 미읽음 수를 그대로 반환한다")
        fun shouldReturnUnreadCount() {
            every { notificationQuery.countUnreadByMemberId(1L) } returns 7L

            val count = service.getUnreadCount(memberId = 1L)

            assertThat(count).isEqualTo(7L)
        }

        @Test
        @DisplayName("미읽음이 없으면 0을 반환한다")
        fun shouldReturnZeroWhenNoUnread() {
            every { notificationQuery.countUnreadByMemberId(1L) } returns 0L

            val count = service.getUnreadCount(memberId = 1L)

            assertThat(count).isEqualTo(0L)
        }
    }
}
