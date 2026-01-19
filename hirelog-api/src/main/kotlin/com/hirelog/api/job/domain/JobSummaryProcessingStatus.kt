package com.hirelog.api.job.domain

/**
 * JobProcessing 상태 정의
 *
 * 역할:
 * - JD 처리 파이프라인의 현재 위치 표현
 * - 비동기 처리 분기 기준
 * - 운영/모니터링/재시도 판단 기준
 *
 * 설계 원칙:
 * - 상태는 "무엇을 하고 있는가"를 나타낸다
 * - 실패 원인은 status가 아니라 errorCode로 표현한다
 */
enum class JobSummaryProcessingStatus {

    /**
     * Job 생성됨
     *
     * - 요청 수신 완료
     * - 아직 어떤 처리도 시작되지 않은 상태
     */
    CREATED,

    /**
     * 전처리 요청 시작
     *
     * - Python 전처리 워커로 요청 발행됨
     * - canonical text / hash 생성 대기
     */
    PREPROCESSING,

    /**
     * 전처리 완료
     *
     * - canonical text / hash 생성 완료
     * - snapshot 생성 가능 상태
     */
    PREPROCESSED,

    /**
     * 중복 JD로 판정됨
     *
     * - canonical hash 기준 중복
     * - 파이프라인 종료 상태
     */
    DUPLICATE,

    /**
     * 요약 가능 상태
     *
     * - 중복 아님
     * - LLM 호출 가능
     */
    READY_FOR_SUMMARY,

    /**
     * 요약 진행 중
     *
     * - LLM 호출 요청 발행됨
     * - 결과 수신 대기
     */
    SUMMARIZING,

    /**
     * 모든 처리 완료
     *
     * - summary 저장 완료
     * - 파이프라인 정상 종료
     */
    COMPLETED,

    /**
     * 전처리 단계 실패
     *
     * - Python 전처리 오류
     * - 타임아웃 / 파싱 실패 / 내부 에러 등
     */
    FAILED_PREPROCESS,

    /**
     * 요약 단계 실패
     *
     * - LLM 호출 실패
     * - 응답 파싱 실패
     * - 비용/쿼터/네트워크 문제 등
     */
    FAILED_SUMMARY
}
