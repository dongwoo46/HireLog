package com.hirelog.api.company.application.port

import com.hirelog.api.company.domain.CompanyCandidate

/**
 * CompanyCandidateCommand
 *
 * 책임:
 * - CompanyCandidate 쓰기 전담
 * - 상태 변경을 위한 엔티티 로딩
 *
 * 원칙:
 * - 변경 목적 조회는 반드시 락 기반
 * - 일반 조회는 Query Port 책임
 */
interface CompanyCandidateCommand {

    /**
     * 신규 생성 / 변경 사항 반영
     *
     * 주의:
     * - 호출자는 트랜잭션을 보장해야 한다
     */
    fun save(candidate: CompanyCandidate): CompanyCandidate

    /**
     * 상태 변경 목적 단건 조회
     *
     * 의미:
     * - approve / reject 전용
     * - 동시 수정 방지를 위한 락 보장
     */
    fun findByIdForUpdate(candidateId: Long): CompanyCandidate?

    /**
     * 승인된 Candidate 조회 (처리 대상)
     *
     * 의미:
     * - Scheduler / Worker 전용
     * - 이후 상태 변경을 전제로 함
     *
     * 제약:
     * - 반드시 락 기반
     * - 처리량 제한 필수
     */
    fun findApprovedForUpdate(limit: Int): List<CompanyCandidate>
}
