package com.hirelog.api.member.presentation.controller

import com.hirelog.api.common.config.security.AuthenticatedMember
import com.hirelog.api.common.config.security.CurrentUser
import com.hirelog.api.member.application.MemberWriteService
import com.hirelog.api.member.application.view.MemberDetailView
import com.hirelog.api.member.infra.persistence.querydsl.MemberJpaQueryDsl
import com.hirelog.api.member.presentation.dto.UpdateProfileReq
import com.hirelog.api.member.presentation.dto.UpdateUsernameReq
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * MemberController
 *
 * 회원 본인용 API
 */
@RestController
@RequestMapping("/api/member")
class MemberController(
    private val memberWriteService: MemberWriteService,
    private val memberQuery: MemberJpaQueryDsl
) {

    /**
     * 내 정보 조회
     */
    @GetMapping("/me")
    fun getMe(
        @CurrentUser member: AuthenticatedMember
    ): ResponseEntity<MemberDetailView> {

        val view = memberQuery.findDetailById(member.memberId)
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(view)
    }

    /**
     * 프로필 수정
     */
    @PatchMapping("/me/profile")
    fun updateProfile(
        @Valid @RequestBody request: UpdateProfileReq,
        @CurrentUser member: AuthenticatedMember
    ): ResponseEntity<Void> {

        memberWriteService.updateProfile(
            memberId = member.memberId,
            currentPositionId = request.currentPositionId,
            careerYears = request.careerYears,
            summary = request.summary
        )

        return ResponseEntity.ok().build()
    }

    /**
     * 표시 이름 변경
     */
    @PatchMapping("/me/username")
    fun updateUsername(
        @Valid @RequestBody request: UpdateUsernameReq,
        @CurrentUser member: AuthenticatedMember
    ): ResponseEntity<Void> {

        memberWriteService.updateDisplayName(
            memberId = member.memberId,
            username = request.username
        )

        return ResponseEntity.ok().build()
    }

    /**
     * 회원 탈퇴
     */
    @DeleteMapping("/me")
    fun withdraw(
        @CurrentUser member: AuthenticatedMember
    ): ResponseEntity<Void> {

        memberWriteService.delete(member.memberId)
        return ResponseEntity.noContent().build()
    }
}
