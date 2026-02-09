package com.hirelog.api.common.presentation.controller

import com.hirelog.api.common.application.sse.SseEmitterManager
import com.hirelog.api.common.config.security.AuthenticatedMember
import com.hirelog.api.common.config.security.CurrentUser
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@RestController
@RequestMapping("/api/sse")
class SseController(
    private val sseEmitterManager: SseEmitterManager
) {

    @GetMapping("/subscribe", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun subscribe(@CurrentUser member: AuthenticatedMember): SseEmitter {
        return sseEmitterManager.subscribe(member.memberId)
    }
}
