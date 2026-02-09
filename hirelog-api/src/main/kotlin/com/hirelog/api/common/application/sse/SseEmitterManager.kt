package com.hirelog.api.common.application.sse

import com.hirelog.api.common.logging.log
import org.springframework.stereotype.Component
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * SSE 연결 관리자
 *
 * 책임:
 * - memberId 기반 SseEmitter 생명주기 관리
 * - 이벤트 전송 (dead emitter 자동 정리)
 *
 * 제약:
 * - In-Memory 기반 → Single Instance 전제
 * - Multi-instance 시 Redis Pub/Sub 확장 필요
 */
@Component
class SseEmitterManager {

    companion object {
        private const val SSE_TIMEOUT_MS = 120_000L // 120초
    }

    private val emitters = ConcurrentHashMap<Long, CopyOnWriteArrayList<SseEmitter>>()

    fun subscribe(memberId: Long): SseEmitter {
        val emitter = SseEmitter(SSE_TIMEOUT_MS)
        val list = emitters.computeIfAbsent(memberId) { CopyOnWriteArrayList() }
        list.add(emitter)

        emitter.onCompletion {
            list.remove(emitter)
            log.debug("[SSE_DISCONNECTED] memberId={}, reason=completion", memberId)
        }
        emitter.onTimeout {
            list.remove(emitter)
            log.debug("[SSE_DISCONNECTED] memberId={}, reason=timeout", memberId)
        }
        emitter.onError {
            list.remove(emitter)
            log.debug("[SSE_DISCONNECTED] memberId={}, reason=error", memberId)
        }

        // 연결 확인용 초기 이벤트 (프록시 503 방지)
        emitter.send(SseEmitter.event().name("connect").data("connected"))

        log.info("[SSE_CONNECTED] memberId={}", memberId)
        return emitter
    }

    fun send(memberId: Long, eventName: String, data: Any) {
        val list = emitters[memberId] ?: return
        val dead = mutableListOf<SseEmitter>()

        for (emitter in list) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data))
            } catch (e: Exception) {
                dead.add(emitter)
                log.debug("[SSE_SEND_FAILED] memberId={}, event={}", memberId, eventName)
            }
        }

        if (dead.isNotEmpty()) {
            list.removeAll(dead.toSet())
        }
    }
}
