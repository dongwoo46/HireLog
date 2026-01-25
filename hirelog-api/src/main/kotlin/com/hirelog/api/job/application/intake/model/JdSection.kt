package com.hirelog.api.job.application.intake.model

/**
 * JD 섹션 정의
 *
 * 가중치 설계 이유:
 * - responsibilities / requirements가 JD 정체성의 핵심
 * - preferred / etc는 보조 정보
 */
enum class JdSection(
    val key: String,
    val weight: Int
) {
    RESPONSIBILITIES("responsibilities", 3),
    REQUIREMENTS("requirements", 3),
    PREFERRED("preferred", 2),
    ETC("etc", 1);

    companion object {
        fun fromKey(key: String): JdSection? =
            entries.find { it.key == key }
    }
}
