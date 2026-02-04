package com.hirelog.api.job.infra.persistence.opensearch

/**
 * JobSummary OpenSearch 인덱스 상수
 */
object JobSummaryIndexConstants {

    const val INDEX_NAME = "job_summary"

    object Fields {
        // ID 필드
        const val ID = "id"
        const val JOB_SNAPSHOT_ID = "jobSnapshotId"
        const val BRAND_ID = "brandId"
        const val COMPANY_ID = "companyId"
        const val POSITION_ID = "positionId"
        const val BRAND_POSITION_ID = "brandPositionId"
        const val POSITION_CATEGORY_ID = "positionCategoryId"

        // 기본 정보
        const val BRAND_NAME = "brandName"
        const val COMPANY_NAME = "companyName"
        const val POSITION_NAME = "positionName"
        const val BRAND_POSITION_NAME = "brandPositionName"
        const val POSITION_CATEGORY_NAME = "positionCategoryName"

        // 경력
        const val CAREER_TYPE = "careerType"
        const val CAREER_YEARS = "careerYears"

        // JD 요약
        const val SUMMARY_TEXT = "summaryText"
        const val RESPONSIBILITIES = "responsibilities"
        const val REQUIRED_QUALIFICATIONS = "requiredQualifications"
        const val PREFERRED_QUALIFICATIONS = "preferredQualifications"
        const val TECH_STACK = "techStack"
        const val TECH_STACK_PARSED = "techStackParsed"
        const val RECRUITMENT_PROCESS = "recruitmentProcess"

        // Insight 필드
        const val IDEAL_CANDIDATE = "idealCandidate"
        const val MUST_HAVE_SIGNALS = "mustHaveSignals"
        const val PREPARATION_FOCUS = "preparationFocus"
        const val TRANSFERABLE_STRENGTHS_AND_GAP_PLAN = "transferableStrengthsAndGapPlan"
        const val PROOF_POINTS_AND_METRICS = "proofPointsAndMetrics"
        const val STORY_ANGLES = "storyAngles"
        const val KEY_CHALLENGES = "keyChallenges"
        const val TECHNICAL_CONTEXT = "technicalContext"
        const val QUESTIONS_TO_ASK = "questionsToAsk"
        const val CONSIDERATIONS = "considerations"

        // 메타
        const val CREATED_AT = "createdAt"

        // Multi-field suffixes
        const val ENGLISH_SUFFIX = "english"
        const val KEYWORD_SUFFIX = "keyword"
    }

    object Analyzers {
        const val NORI = "nori_analyzer"
        const val ENGLISH = "english_analyzer"
    }
}
