package com.hirelog.api.member.domain.vo

import jakarta.persistence.Column
import jakarta.persistence.Embeddable

@Embeddable
class ReviewContent(

    /**
     * JD를 보고 느낀 점
     * 예: 생각보다 빡셈, 재밌어 보임
     */
    @Column(name = "feeling", length = 1000)
    val feeling: String? = null,

    /**
     * 지원 팁 / 조언
     * 예: 이 회사는 CS 위주로 준비하는 게 좋음
     */
    @Column(name = "tip", length = 1000)
    val tip: String? = null,

    /**
     * 개인 경험
     * 예: 예전에 비슷한 포지션 인터뷰 경험
     */
    @Column(name = "experience", length = 1000)
    val experience: String? = null
) {

    init {
        require(
            feeling != null || tip != null || experience != null
        ) {
            "ReviewContent must have at least one field"
        }
    }
}
