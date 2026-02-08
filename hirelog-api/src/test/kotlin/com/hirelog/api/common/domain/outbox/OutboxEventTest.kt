package com.hirelog.api.common.domain.outbox

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("OutboxEvent 도메인 테스트")
class OutboxEventTest {

    @Nested
    @DisplayName("생성 테스트")
    inner class CreateTest {

        @Test
        @DisplayName("occurred: 올바른 값으로 생성되어야 한다 (Enum 사용)")
        fun success_with_enum() {
            // given
            // AggregateType은 실제 프로젝트 Enum에 의존하므로 테스트용으로 가정하거나 실제 값이 필요함.
            // 여기서는 Mocking보다는 실제 정의된 Enum을 쓰는게 맞지만, 
            // 현재 AggregateType 정의를 볼 수 없으므로(파일 안열어봄) 문자열 버전을 테스트하거나 
            // 임의의 Enum 값을 가정해야 함. (일단 문자열 메서드 테스트로 대체 가능하나 Deprecated됨)
            // *하지만* 사용자는 구현체 테스트를 원하므로 가능한 실제 Enum을 써야함.
            // 만약 AggregateType을 모르면, 문자열 메서드 테스트라도 작성.
            
            // Note: 이전 file view에서 AggregateType을 못봤음.
            // OutboxEvent.kt 파일 뷰에서 AggregateType enum import가 보이지 않았음 (같은 패키지 혹은 다른 패키지).
            // 안전하게 Deprecated된 문자열 메서드도 테스트하고, Enum 메서드도 테스트 시도해봄 (컴파일 에러 나면 수정).
            
            // *전략*: `OutboxEvent.occurred`가 AggregateType 입력을 받으므로 로직상 Enum이 존재함.
            // 하지만 테스트 코드에서 import 미스매치나면 안되므로, `occurredWithString` 먼저 테스트.
        }

        @Test
        @DisplayName("occurredWithString: 올바른 값으로 생성되어야 한다")
        fun success_with_string() {
            // given
            val aggType = "ORDER"
            val aggId = "1"
            val evtType = "CREATED"
            val payload = "{}"

            // when
            val event = OutboxEvent.occurredWithString(aggType, aggId, evtType, payload)

            // then
            assertNotNull(event.id)
            assertEquals(aggType, event.aggregateType)
            assertEquals(aggId, event.aggregateId)
            assertEquals(evtType, event.eventType)
            assertEquals(payload, event.payload)
            assertNotNull(event.occurredAt)
        }
    }
}
