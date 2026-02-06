package com.hirelog.api.company.application

import com.hirelog.api.company.application.port.CompanyCandidateCommand
import com.hirelog.api.company.application.port.CompanyCommand
import com.hirelog.api.company.domain.Company
import com.hirelog.api.company.domain.CompanyCandidate
import com.hirelog.api.company.domain.CompanySource
import com.hirelog.api.job.application.summary.port.JobSummaryCommand
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class CompanyCandidateProcessingService(
    private val candidateCommand: CompanyCandidateCommand,
    private val companyCommand: CompanyCommand,
    private val jobSummaryCommand: JobSummaryCommand
) {

    /**
     * 1단계: APPROVED → PROCESSING 확보
     *
     * 역할:
     * - 스케줄러가 처리 대상 선점
     * - 다중 인스턴스 환경에서 중복 처리 방지
     */
    @Transactional
    fun fetchAndMarkProcessing(limit: Int): List<CompanyCandidate> {
        val candidates = candidateCommand.findApprovedForUpdate(limit)
        candidates.forEach { it.markProcessing() }
        return candidates
    }

    /**
     * 2단계: 실제 처리 진입점
     *
     * 주의:
     * - 여기서는 트랜잭션을 열지 않는다
     * - 실패 시 반드시 FAILED 상태를 남긴다
     */
    fun process(candidate: CompanyCandidate) {
        try {
            doProcess(candidate)
        } catch (ex: Exception) {
            markFailed(candidate.id, ex)
            throw ex
        }
    }

    /**
     * 실제 비즈니스 처리
     *
     * 트랜잭션:
     * - 성공 시에만 COMMIT
     * - 예외 발생 시 전체 롤백
     */
    @Transactional
    fun doProcess(candidate: CompanyCandidate) {

        // 1️⃣ Company 확보 (이미 존재하면 재사용)
        val company = companyCommand.findByNormalizedName(candidate.normalizedName)
            ?: companyCommand.save(
                Company.create(
                    name = candidate.candidateName,
                    source = CompanySource.LLM,
                    externalId = null
                )
            )

        // 2️⃣ JobSummary 조회 (상태 변경 목적)
        val jobSummary = jobSummaryCommand.findById(candidate.jdSummaryId)
            ?: error("JobSummary not found. id=${candidate.jdSummaryId}")

        // 3️⃣ JobSummary에 Company 반영 (도메인 행위)
        jobSummary.applyCompany(
            companyId = company.id,
            companyName = company.name
        )

        // 4️⃣ 변경 사항 반영
        jobSummaryCommand.update(jobSummary)

        // 5️⃣ Candidate 완료 처리
        candidate.complete()
    }

    /**
     * 실패 상태 기록 (롤백되면 안 됨)
     *
     * 핵심:
     * - 반드시 별도 트랜잭션(REQUIRES_NEW)
     * - PROCESSING → FAILED 보장
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun markFailed(candidateId: Long, ex: Exception) {
        val candidate = candidateCommand.findByIdForUpdate(candidateId)
            ?: return
        candidate.fail()
    }
}
