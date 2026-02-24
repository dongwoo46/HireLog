package com.hirelog.api.member.domain.policy

import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.*

@DisplayName("StrictUsernameValidationPolicy 테스트")
class StrictUsernameValidationPolicyTest {

    private val policy = StrictUsernameValidationPolicy

    @Nested
    @DisplayName("validate는")
    inner class ValidateTest {

        @Test
        @DisplayName("정상 닉네임은 예외 없이 통과한다")
        fun shouldPassWithValidUsername() {
            assertThatCode { policy.validate("toss_backend") }.doesNotThrowAnyException()
            assertThatCode { policy.validate("개발자123") }.doesNotThrowAnyException()
            assertThatCode { policy.validate("BackendDev") }.doesNotThrowAnyException()
        }

        @Test
        @DisplayName("blank 닉네임은 예외를 던진다")
        fun shouldThrowWhenBlank() {
            assertThatThrownBy { policy.validate("") }
                .isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        @DisplayName("예약어 ADMIN은 대소문자 무관하게 차단된다")
        fun shouldBlockAdminInAnyCase() {
            assertThatThrownBy { policy.validate("ADMIN") }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("닉네임은 사용할 수 없습니다")

            assertThatThrownBy { policy.validate("admin") }
                .isInstanceOf(IllegalArgumentException::class.java)

            assertThatThrownBy { policy.validate("Admin") }
                .isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        @DisplayName("예약어 ROOT, SYSTEM, 관리자는 차단된다")
        fun shouldBlockOtherReservedWords() {
            listOf("ROOT", "SYSTEM", "관리자").forEach { reserved ->
                assertThatThrownBy { policy.validate(reserved) }
                    .isInstanceOf(IllegalArgumentException::class.java)
            }
        }

        @Test
        @DisplayName("금지 단어가 포함된 닉네임은 차단된다")
        fun shouldBlockBannedWords() {
            assertThatThrownBy { policy.validate("fuck_you") }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("부적절한 표현")

            assertThatThrownBy { policy.validate("shit123") }
                .isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        @DisplayName("금지 단어가 특수문자로 분리된 경우도 정규화 후 차단된다")
        fun shouldBlockBannedWordNormalized() {
            // 정규화: 소문자 + 비허용문자 제거
            assertThatThrownBy { policy.validate("f-u-c-k") }
                .isInstanceOf(IllegalArgumentException::class.java)
        }
    }
}
