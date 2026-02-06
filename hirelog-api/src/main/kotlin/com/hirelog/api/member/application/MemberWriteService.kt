package com.hirelog.api.member.application

import com.hirelog.api.auth.domain.OAuthUser
import com.hirelog.api.common.config.properties.AdminProperties
import com.hirelog.api.common.exception.EntityNotFoundException
import com.hirelog.api.member.application.port.MemberCommand
import com.hirelog.api.member.application.port.MemberQuery
import com.hirelog.api.member.domain.Member
import com.hirelog.api.member.domain.MemberRole
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Member Write Application Service
 *
 * 책임:
 * - Member 쓰기 유스케이스 전담
 * - 트랜잭션 경계 정의
 * - 도메인 상태 변경 트리거
 */
@Service
class MemberWriteService(
    private val memberCommand: MemberCommand,
    private val memberQuery: MemberQuery,
    private val adminProperties: AdminProperties
) {

    /**
     * 기존 회원 + OAuth 계정 연결
     */
    @Transactional
    fun bindOAuthAccount(
        email: String,
        oAuthUser: OAuthUser,
    ): Member {
        val member = memberCommand.findByEmail(email)
            ?: throw EntityNotFoundException(
                entityName = "Member",
                identifier = email
            )

        member.linkOAuthAccount(
            provider = oAuthUser.provider,
            providerUserId = oAuthUser.providerUserId,
        )

        return member
    }

    /**
     * 신규 회원 가입 + OAuth 계정 연결
     */
    @Transactional
    fun signupWithOAuth(
        email: String,
        username: String,
        oAuthUser: OAuthUser,
        currentPositionId: Long? = null,
        careerYears: Int? = null,
        summary: String? = null,
    ): Member {
        if (memberQuery.existsActiveByUsername(username)) {
            throw IllegalArgumentException("이미 사용 중인 username 입니다.")
        }

        if (memberQuery.existsActiveByEmail(email)) {
            throw IllegalArgumentException("이미 사용 중인 email 입니다.")
        }

        val member = Member.createByOAuth(
            email = email,
            username = username,
            provider = oAuthUser.provider,
            providerUserId = oAuthUser.providerUserId,
            currentPositionId = currentPositionId,
            careerYears = careerYears,
            summary = summary,
        )

        if (adminProperties.isAdmin(email)) {
            member.grantAdmin()
        }

        return memberCommand.save(member)
    }

    /**
     * 계정 복구 완료
     *
     * - username 재입력 필수
     * - 중복 체크 수행
     * - 상태 ACTIVE 전환
     */
    @Transactional
    fun recoveryAccount(
        memberId: Long,
        email: String,
        username: String,
        currentPositionId: Long?,
        careerYears: Int?,
        summary: String?
    ): Member {

        if (memberQuery.existsActiveByUsername(username)) {
            throw IllegalArgumentException("이미 사용 중인 username 입니다.")
        }

        if (memberQuery.existsActiveByEmail(email)) {
            throw IllegalArgumentException("이미 사용 중인 email 입니다.")
        }

        val member = getRequired(memberId)

        member.updateDisplayName(username)
        member.updateProfile(
            currentPositionId = currentPositionId,
            careerYears = careerYears,
            summary = summary
        )

        member.activate()

        return member
    }


    /**
     * 표시 이름 변경
     */
    @Transactional
    fun updateDisplayName(memberId: Long, username: String) {
        val member = getRequired(memberId)
        member.updateDisplayName(username)
    }

    /**
     * 프로필 수정
     */
    @Transactional
    fun updateProfile(
        memberId: Long,
        currentPositionId: Long?,
        careerYears: Int?,
        summary: String?,
    ) {
        val member = getRequired(memberId)
        member.updateProfile(
            currentPositionId = currentPositionId,
            careerYears = careerYears,
            summary = summary,
        )
    }

    /**
     * 계정 정지 (Admin)
     */
    @Transactional
    fun suspend(memberId: Long) {
        val member = getRequired(memberId)
        member.suspend()
    }

    /**
     * 계정 활성화 (Admin)
     */
    @Transactional
    fun activate(memberId: Long) {
        val member = getRequired(memberId)
        member.activate()
    }

    /**
     * 계정 탈퇴 (논리 삭제)
     */
    @Transactional
    fun delete(memberId: Long) {
        val member = getRequired(memberId)
        member.softDelete()
    }

    private fun getRequired(memberId: Long): Member =
        memberCommand.findById(memberId)
            ?: throw EntityNotFoundException(
                entityName = "Member",
                identifier = memberId
            )
}
