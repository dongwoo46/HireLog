package com.hirelog.api.company.application.port

import com.hirelog.api.company.domain.CompanyCandidate

/**
 * CompanyCandidateCommand
 *
 * 책임:
 * - CompanyCandidate 변경/저장 포트
 * - 쓰기 모델 관점
 */
interface CompanyCandidateCommand {

    /**
     * 신규 후보 저장
     */
    fun save(candidate: CompanyCandidate): CompanyCandidate

    /**
     * 수정 목적 단건 조회
     *
     * 주의:
     * - 반드시 트랜잭션 내부에서 사용
     */
    fun findByIdForUpdate(id: Long): CompanyCandidate?

    fun findByNormalizedName(normalizedName: String): CompanyCandidate?
}
