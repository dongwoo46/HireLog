package com.hirelog.api.notification.presentation.dto

import com.hirelog.api.notification.domain.type.NotificationReferenceType
import com.hirelog.api.notification.domain.type.NotificationType

/**
 * 알림 생성 요청 DTO
 *
 * 책임:
 * - 외부 API 계약 모델
 * - Validation은 Presentation 레이어에서 처리
 */
data class CreateNotificationReq(

    val memberId: Long,

    val type: NotificationType,

    val title: String,

    val message: String? = null,

    val referenceType: NotificationReferenceType? = null,

    val referenceId: Long? = null,

    val metadata: Map<String, Any?> = emptyMap()
)