/**
 * JobSummaryLlmRawResult
 *
 * LLM 원본 응답 중 "요약 도메인으로 변환 가능한 최소 단위"
 *
 * - 모든 필드는 nullable
 * - enum / LocalDate 사용 ❌
 * - 정규화 ❌
 */
data class JobSummaryLlmRawResult(

    // 커리어 구분 (예: "신입", "경력", "무관")
    val careerType: String?,

    // 경력 연차 원문 (예: "3년 이상", "신입", "무관")
    val careerYears: String?,

    // JD 전체 요약
    val summary: String?,

    // 주요 업무
    val responsibilities: String?,

    // 필수 자격 요건
    val requiredQualifications: String?,

    // 우대 사항
    val preferredQualifications: String?,

    // 기술 스택 (자유 텍스트)
    val techStack: String?,

    // 채용 절차 (선택)
    val recruitmentProcess: String?,

    val brandName: String?,
    val positionName: String?
)
