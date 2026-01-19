package com.hirelog.api.member.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "member_profile")
class MemberProfile(

    @Id
    @Column(name = "member_id")
    val memberId: Long,

    /**
     * 현재 직무 (공통 Position)
     */
    @Column(name = "current_position_id")
    val currentPositionId: Long? = null,

    /**
     * 총 경력 연차
     */
    @Column(name = "career_years")
    val careerYears: Int? = null,

    /**
     * 간단 자기소개 / 요약
     */
    @Column(name = "summary", length = 1000)
    val summary: String? = null
)
