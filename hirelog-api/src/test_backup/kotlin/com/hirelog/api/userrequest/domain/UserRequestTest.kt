package com.hirelog.api.userrequest.domain

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class UserRequestTest {

    @Nested
    @DisplayName("create 테스트")
    inner class CreateTest {

        @Test
        @DisplayName("UserRequest 생성 시 기본 상태는 OPEN")
        fun `should create UserRequest with OPEN status`() {
            // when
            val userRequest = UserRequest.create(
                memberId = 1L,
                requestType = UserRequestType.ERROR_REPORT,
                content = "테스트 내용"
            )

            // then
            assertEquals(1L, userRequest.memberId)
            assertEquals(UserRequestType.ERROR_REPORT, userRequest.requestType)
            assertEquals("테스트 내용", userRequest.content)
            assertEquals(UserRequestStatus.OPEN, userRequest.status)
            assertNull(userRequest.resolvedAt)
        }

        @Test
        @DisplayName("다양한 requestType으로 생성 가능")
        fun `should create UserRequest with various request types`() {
            // given
            val types = UserRequestType.entries

            // when & then
            types.forEach { type ->
                val userRequest = UserRequest.create(
                    memberId = 1L,
                    requestType = type,
                    content = "내용"
                )
                assertEquals(type, userRequest.requestType)
            }
        }
    }

    @Nested
    @DisplayName("updateStatus 테스트")
    inner class UpdateStatusTest {

        @Test
        @DisplayName("RESOLVED 상태로 변경 시 resolvedAt 자동 설정")
        fun `should set resolvedAt when status changed to RESOLVED`() {
            // given
            val userRequest = UserRequest.create(
                memberId = 1L,
                requestType = UserRequestType.FEATURE_REQUEST,
                content = "기능 요청"
            )
            assertNull(userRequest.resolvedAt)

            // when
            userRequest.updateStatus(UserRequestStatus.RESOLVED)

            // then
            assertEquals(UserRequestStatus.RESOLVED, userRequest.status)
            assertNotNull(userRequest.resolvedAt)
        }

        @Test
        @DisplayName("REJECTED 상태로 변경 시 resolvedAt 자동 설정")
        fun `should set resolvedAt when status changed to REJECTED`() {
            // given
            val userRequest = UserRequest.create(
                memberId = 1L,
                requestType = UserRequestType.MODIFY_REQUEST,
                content = "수정 요청"
            )

            // when
            userRequest.updateStatus(UserRequestStatus.REJECTED)

            // then
            assertEquals(UserRequestStatus.REJECTED, userRequest.status)
            assertNotNull(userRequest.resolvedAt)
        }

        @Test
        @DisplayName("IN_PROGRESS 상태로 변경 시 resolvedAt 설정 안됨")
        fun `should not set resolvedAt when status changed to IN_PROGRESS`() {
            // given
            val userRequest = UserRequest.create(
                memberId = 1L,
                requestType = UserRequestType.REPROCESS_REQUEST,
                content = "재처리 요청"
            )

            // when
            userRequest.updateStatus(UserRequestStatus.IN_PROGRESS)

            // then
            assertEquals(UserRequestStatus.IN_PROGRESS, userRequest.status)
            assertNull(userRequest.resolvedAt)
        }

        @Test
        @DisplayName("OPEN 상태로 변경 시 resolvedAt 설정 안됨")
        fun `should not set resolvedAt when status changed to OPEN`() {
            // given
            val userRequest = UserRequest.create(
                memberId = 1L,
                requestType = UserRequestType.ERROR_REPORT,
                content = "오류 신고"
            )
            userRequest.updateStatus(UserRequestStatus.IN_PROGRESS)

            // when
            userRequest.updateStatus(UserRequestStatus.OPEN)

            // then
            assertEquals(UserRequestStatus.OPEN, userRequest.status)
            assertNull(userRequest.resolvedAt)
        }
    }
}
