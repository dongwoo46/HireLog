package com.hirelog.api.job.infra.persistence.jpa.repository

import com.hirelog.api.job.domain.model.JobSnapshot
import com.hirelog.api.job.domain.model.QJobSnapshot
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
class JobSnapshotJpaQueryDslRepositoryImpl(
    private val queryFactory: JPAQueryFactory
) : JobSnapshotJpaQueryDslRepository {

    /**
     * 입력된 기간(openedDate / closedDate)과
     * "기간이 겹치는" JobSnapshot Entity 목록 조회
     *
     * 책임:
     * - 기간 겹침 조건에 따른 Snapshot 후보 필터링
     *
     * 비책임:
     * - 중복 여부 판단 ❌
     * - 비즈니스 정책 적용 ❌
     *
     * 설계 포인트:
     * - openedDate / closedDate는 nullable
     * - null 조건은 QueryDSL에서 자동으로 제거된다
     */
    override fun findAllOverlappingDateRange(
        openedDate: LocalDate?,
        closedDate: LocalDate?
    ): List<JobSnapshot> {

        // QueryDSL 메타 모델
        // 문자열 기반 필드 접근 대신 타입 안전한 접근을 제공
        val s = QJobSnapshot.jobSnapshot

        return queryFactory
            .selectFrom(s)
            .where(
                // 입력 openedDate 기준으로 겹치는 Snapshot 필터
                // openedDate가 null이면 조건 자체가 제거됨
                overlapWithOpenedDate(s, openedDate),

                // 입력 closedDate 기준으로 겹치는 Snapshot 필터
                // closedDate가 null이면 조건 자체가 제거됨
                overlapWithClosedDate(s, closedDate)
            )
            .fetch()
    }

    /**
     * 입력 openedDate 기준 겹침 조건 생성
     *
     * 의미:
     * - Snapshot이 입력 openedDate 이후까지 "유효한 경우"만 포함
     *
     * 조건 해석:
     * - snapshot.closedDate IS NULL
     *   → 상시채용, 종료되지 않은 공고
     *
     * - snapshot.closedDate >= openedDate
     *   → 입력 시작일 이후까지 열려 있는 공고
     *
     * openedDate가 null이면:
     * - 조건 자체를 적용하지 않기 위해 null 반환
     * - QueryDSL where 절에서 자동 제외됨
     */
    private fun overlapWithOpenedDate(
        s: QJobSnapshot,
        openedDate: LocalDate?
    ): BooleanExpression? {
        return openedDate?.let {
            s.closedDate.isNull
                .or(s.closedDate.goe(it))
        }
    }

    /**
     * 입력 closedDate 기준 겹침 조건 생성
     *
     * 의미:
     * - Snapshot 시작일이 입력 종료일보다 늦지 않은 경우만 포함
     *
     * 조건 해석:
     * - snapshot.openedDate <= closedDate
     *   → 입력 기간이 끝나기 전에 시작된 공고
     *
     * closedDate가 null이면:
     * - 조건 자체를 적용하지 않기 위해 null 반환
     * - QueryDSL where 절에서 자동 제외됨
     */
    private fun overlapWithClosedDate(
        s: QJobSnapshot,
        closedDate: LocalDate?
    ): BooleanExpression? {
        return closedDate?.let {
            s.openedDate.loe(it)
        }
    }
}
