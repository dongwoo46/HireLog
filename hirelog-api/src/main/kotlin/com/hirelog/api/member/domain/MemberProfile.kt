package com.hirelog.api.member.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * MemberProfile
 *
 * 설계 의도:
 * - Member의 확장 정보(1:1) 역할
 * - PK = memberId (Member.id)
 * - JPA 연관관계(@OneToOne)는 사용하지 않는다
 *   → 느슨한 결합 유지, 불필요한 JOIN 방지
 * - memberId 기반으로만 조회/갱신한다
 *
 * 주의:
 * - member_profile.member_id는 DB 레벨에서 FK로 관리할 수 있다
 */
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
