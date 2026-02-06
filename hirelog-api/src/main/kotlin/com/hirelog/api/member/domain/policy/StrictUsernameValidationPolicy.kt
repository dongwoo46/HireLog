package com.hirelog.api.member.domain.policy

/**
 * 일반 사용자용 Username 검증 정책
 */
object StrictUsernameValidationPolicy : UsernameValidationPolicy {

    private val RESERVED_USERNAMES = setOf(
        "ADMIN",
        "ROOT",
        "SYSTEM",
        "관리자"
    )

    private val BANNED_WORDS = setOf(
        "씨발", "시발", "좆", "병신", "개새끼", "새끼",
        "미친놈", "미친년",
        "멍청", "등신", "병자", "폐인",
        "장애인", "장애", "정신병",
        "fuck", "shit", "bitch", "asshole", "bastard",
        "fucking", "motherfucker"
    )

    override fun validate(username: String) {
        require(username.isNotBlank())

        val upper = username.uppercase()
        require(!RESERVED_USERNAMES.contains(upper)) {
            "해당 닉네임은 사용할 수 없습니다."
        }

        require(!containsBannedWord(username)) {
            "부적절한 표현이 포함된 닉네임은 사용할 수 없습니다."
        }
    }

    private fun containsBannedWord(username: String): Boolean {
        val normalized = normalize(username)
        return BANNED_WORDS.any { normalized.contains(it) }
    }

    private fun normalize(input: String): String {
        return input
            .lowercase()
            .replace(Regex("[^a-z0-9가-힣]"), "")
    }
}
