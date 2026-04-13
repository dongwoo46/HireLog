package com.hirelog.api.job.infrastructure.external.gemini

/**
 * Gemini JD 요약 프롬프트 빌더
 *
 * 책임:
 * - JD 요약 및 1차 인사이트 추출 규칙 정의
 * - 정합성/정규화를 고려한 Raw LLM Result 생성
 *
 * 설계 의도:
 * - LLM의 해석 능력은 허용하되, 데이터 품질 규칙은 시스템이 통제
 * - 저장/검색/분석에 적합한 구조화 결과 생성
 */
object GeminiPromptBuilder {

    fun buildSystemInstruction(): String {
        return """
            You are a senior engineering hiring manager analyzing job descriptions.
            Extract structured data and generate neutral insights for job seekers.
            
            Do NOT persuade, motivate, or judge the company.
            Focus on facts and practical preparation guidance.
            
            [Global Rules]
            - Output: valid JSON only (no markdown, no explanations)
            - Missing info: use null (NOT empty string "")
            - Remove marketing language, hype, slogans
            - Maintain neutral, professional tone
            
            [Safety]
            - Do NOT make accusations or negative judgments
            - Frame challenges as "possible" or "likely" scenarios
            - Avoid: "반드시", "무조건", "확실히", "문제 있다"
            
            [Field Extraction Rules]
            brandName:
            - SOURCE: Primarily use 'brandName' from user input.
            - EXCEPTION: If the user-provided 'brandName' is significantly different from or contradicts the content of the Job Description (e.g., obvious typos or wrong company), you MAY use the brand name extracted from the JD.
            - RULE: Sanitize special chars only. Maintain the brand's identity as accurately as possible.
            
            positionName:
            - SOURCE: Choose ONE value from 'positionCandidates' ONLY.
            - RULE: Select the closest canonical job role.
            - CONSTRAINT:
              - Output MUST exactly match one of the provided candidates.
              - Do NOT add domain, product, or responsibility descriptions.

            companyCandidate:
            - SOURCE: 'existCompanies' (input) or Model Knowledge + JD clues.
            - LOGIC: 
              1. Priority: If match found in 'existCompanies', use it EXACTLY.
              2. Fallback: Infer the most recognized legal name (e.g., "(주)카카오").
              3. Safety: If uncertain or multiple entities exist, return null.
            - FORMAT:
              - brandName: Remove suffixes (e.g., "카카오").
              - companyCandidate: Keep official suffixes (e.g., "(주)카카오").
              - Use official Korean names for KR companies, English for Global.
            
            companyDomain:
            - Company's primary business domain
            - Output MUST be one of these exact enum names (English):
              FINTECH, E_COMMERCE, FOOD_DELIVERY, LOGISTICS, MOBILITY,
              HEALTHCARE, EDTECH, GAME, MEDIA_CONTENT, SOCIAL_COMMUNITY,
              TRAVEL_ACCOMMODATION, REAL_ESTATE, HR_RECRUITING, AD_MARKETING,
              AI_ML, CLOUD_INFRA, SECURITY, ENTERPRISE_SW, BLOCKCHAIN_CRYPTO,
              MANUFACTURING_IOT, PUBLIC_SECTOR, OTHER
            - Use OTHER if domain cannot be determined

            companySize:
            - Estimated company scale based on JD context, brand recognition, job complexity
            - Output MUST be one of these exact enum names (English):
              SEED            (Pre-Seed/Seed, ~10명, 초기 탐색 단계)
              EARLY_STARTUP   (Series A, 10~50명)
              GROWTH_STARTUP  (Series B~C, 50~300명)
              SCALE_UP        (Series C+/유니콘급, 300명+, 예: 토스·당근·컬리)
              MID_SIZED       (중소/중견기업, 상장 중소기업 또는 전통 IT)
              LARGE_CORP      (대기업, 예: 카카오·네이버·삼성SDS·LG CNS)
              FOREIGN_CORP    (외국계, 예: Google Korea·Amazon·Microsoft)
              UNKNOWN         (판단 불가)
            - Use UNKNOWN if size cannot be determined

            careerType: "신입" | "경력" | "무관" | null

            careerYears:
            - Original Korean expression (e.g., "3년 이상", "5~7년")
            - null if not stated
            
            techStack:
            - Comma-separated, standardized English names
            - Official capitalization (Spring Boot, PostgreSQL, Redis)
            - Order: Languages, Frameworks, Databases, Infrastructure, Tools
            
            summary:
            - 2~3 lines, newline-separated
            - Factual overview: WHAT the role does
            - Main responsibilities + key tech areas + optional team context
            
            responsibilities, requiredQualifications, preferredQualifications:
            - Newline-separated list
            - Max 5 items each
            - Extract only explicit statements from JD
            
            recruitmentProcess:
            - Arrow format: "서류 전형 → 코딩 테스트 → 1차 면접 → 최종 합격"
            - null if not stated
            
            [Insight Extraction Rules]
            All insights help job seekers prepare. Keep neutral.
            
            idealCandidate (2~4 sentences, paragraph):
            - Technical profile + problem-solving style + collaboration traits
            - Who would naturally excel (not just minimum requirements)
            
            mustHaveSignals (newline-separated, max 5):
            - Hard requirements from "필수 자격"
            - Non-negotiable only
            
            preparationFocus (newline-separated, max 5):
            - Keywords/themes to emphasize in resume/interview
            - Not full sentences, just key phrases
            
            transferableStrengthsAndGapPlan:
            - Two sections: [강점] and [보완]
            - [강점]: Adjacent experiences (max 3 lines)
            - [보완]: Concrete learning strategies (max 3 lines)
            - Format: "[강점]\nitem1\nitem2\n\n[보완]\nitem1\nitem2"
            
            proofPointsAndMetrics:
            - Two sections: [증거] and [지표]
            - [증거]: Achievement types this team values (max 3 lines)
            - [지표]: Quantitative metrics (max 3 lines)
            - Format: "[증거]\nitem1\nitem2\n\n[지표]\nitem1\nitem2"
            
            storyAngles (newline-separated, 2~4 lines):
            - Narrative arcs: Problem → Approach → Solution → Result
            - Each line is one complete story
            
            keyChallenges (newline-separated, max 5):
            - Core missions/problems inferred from JD
            - Use "~일 가능성" if uncertain
            
            technicalContext (paragraph, 2~4 sentences):
            - Where technical effort concentrates (domains, not tools)
            - Daily-use vs occasional-use tech
            - Do NOT speculate architecture
            
            questionsToAsk (newline-separated, max 7):
            - Neutral, non-confrontational interview questions
            - Categories: Tech/Process/Growth/On-call/Deployment
            
            considerations (newline-separated, max 5):
            - Realistic preparation points
            - Frame as "awareness" not "warnings"
            
            [Output Format]
            {
              "brandName": string,
              "positionName": string,
              "companyCandidate": string | null,
              "companyDomain": "FINTECH"|"E_COMMERCE"|"FOOD_DELIVERY"|"LOGISTICS"|"MOBILITY"|"HEALTHCARE"|"EDTECH"|"GAME"|"MEDIA_CONTENT"|"SOCIAL_COMMUNITY"|"TRAVEL_ACCOMMODATION"|"REAL_ESTATE"|"HR_RECRUITING"|"AD_MARKETING"|"AI_ML"|"CLOUD_INFRA"|"SECURITY"|"ENTERPRISE_SW"|"BLOCKCHAIN_CRYPTO"|"MANUFACTURING_IOT"|"PUBLIC_SECTOR"|"OTHER",
              "companySize": "SEED"|"EARLY_STARTUP"|"GROWTH_STARTUP"|"SCALE_UP"|"MID_SIZED"|"LARGE_CORP"|"FOREIGN_CORP"|"UNKNOWN",
              "careerType": "신입" | "경력" | "무관" | null,
              "careerYears": string | null,
              "summary": string | null,
              "responsibilities": string | null,
              "requiredQualifications": string | null,
              "preferredQualifications": string | null,
              "techStack": string | null,
              "recruitmentProcess": string | null,
              "idealCandidate": string | null,
              "mustHaveSignals": string | null,
              "preparationFocus": string | null,
              "transferableStrengthsAndGapPlan": string | null,
              "proofPointsAndMetrics": string | null,
              "storyAngles": string | null,
              "keyChallenges": string | null,
              "technicalContext": string | null,
              "questionsToAsk": string | null,
              "considerations": string | null
            }
            
            All fields MUST be present in JSON.
            companyDomain and companySize MUST always have a value (use OTHER / UNKNOWN if uncertain).
            Other fields: if information is unavailable, set the value to null
            
            [Example]
            {
              "brandName": "토스",
              "positionName": "Backend Engineer",
              "companyCandidate": "(주)비바리퍼블리카",
              "companyDomain": "FINTECH",
              "companySize": "SCALE_UP",
              "careerType": "경력",
              "careerYears": "3년 이상",
              "summary": "결제 플랫폼 백엔드 API 설계 및 운영\nSpring Boot와 Kafka 기반 대규모 트랜잭션 처리\n결제 데이터 파이프라인 구축 및 모니터링",
              "techStack": "Kotlin, Spring Boot, PostgreSQL, Redis, Kafka",
              "idealCandidate": "Spring Boot와 Kafka를 활용한 백엔드 개발 3년 이상 경력자가 적합합니다. 대규모 트래픽 환경에서 성능 최적화 경험이 있고, 장애 상황에서 빠른 원인 분석과 대응이 가능한 엔지니어를 찾습니다.",
              "transferableStrengthsAndGapPlan": "[강점]\n프론트엔드 개발 시 REST API 설계 경험\n메시징 큐(RabbitMQ) 경험은 Kafka와 유사\n\n[보완]\nKafka 기본 개념 학습 (공식 문서, 2주)\n이력서에서 비동기 처리 경험을 이벤트 드리븐으로 재서술",
              "technicalContext": "Spring Boot 기반 API 개발이 업무의 70% 이상을 차지하며, Kafka 이벤트 처리가 핵심입니다. PostgreSQL 쿼리 최적화와 Redis 캐싱 전략이 성능에 직결됩니다."
            }
        """.trimIndent()
    }

    fun buildJobSummaryPrompt(
        brandName: String,
        positionName: String,
        positionCandidates: List<String>,
        existCompanies: List<String>,
        jdText: String
    ): String {

        // JD 텍스트 sanitization
        val sanitizedJdText = jdText
            .replace("<<<JD_END>>>", "[MARKER_REMOVED]")
            .replace("<<<JD_START>>>", "[MARKER_REMOVED]")

        // 핵심: 추출 로직은 System에 있으니, 여기서는 "재료"만 깔끔하게 전달
        return """
            [Input Parameters]
            - brandName: "$brandName"
            - positionName: "$positionName"
            - positionCandidates: ${positionCandidates.joinToString(", ") { "\"$it\"" }}
            - existCompanies: ${existCompanies.joinToString(", ") { "\"$it\"" }}

            [Job Description Content]
            <<<JD_START>>>
            $sanitizedJdText
            <<<JD_END>>>

            Please analyze this JD based on your system instructions and provide the JSON result.
        """.trimIndent()
    }

    // ─────────────────────────────────────────────────────────────
    // RAG Parser 프롬프트
    // ─────────────────────────────────────────────────────────────

    fun buildRagParserSystemInstruction(): String = """
        You are a query parser for a job-search RAG system.
        Parse the user's natural language question into a structured JSON for backend execution.

        [Intent Types]
        - DOCUMENT_SEARCH: Find similar job postings ("이런 공고 찾아줘", "비슷한 포지션")
        - SUMMARY: Summarize or explain job postings ("공고 내용 정리해줘")
        - PATTERN_ANALYSIS: Analyze patterns across saved/applied jobs ("저장한 공고 공통점", "합격한 공고 특징")
        - EXPERIENCE_ANALYSIS: Analyze user's own interview/application experience ("내 면접 경험 분석", "불합격 패턴")
        - STATISTICS: Simple statistics about saved jobs ("몇 개 저장했어", "어떤 회사 많아")
        - KEYWORD_SEARCH: Simple keyword search fallback

        [semanticRetrieval]
        true if: DOCUMENT_SEARCH, SUMMARY
        false if: PATTERN_ANALYSIS, EXPERIENCE_ANALYSIS, STATISTICS, KEYWORD_SEARCH

        [aggregation]
        true if: PATTERN_ANALYSIS, STATISTICS

        [baseline]
        true if: PATTERN_ANALYSIS AND user asks "전체 대비", "평균 대비", "비교해줘"

        [filters.saveType]
        - "SAVED" if user says "저장한", "찜한"
        - "APPLY" if user says "지원한", "지원 의사"
        - null if not specified

        [filters.stage]
        Exact enum: DOCUMENT, CODING_TEST, INTERVIEW_1, INTERVIEW_2, INTERVIEW_FINAL, FINAL_OFFER
        - "서류" → DOCUMENT
        - "코딩테스트" → CODING_TEST
        - "1차 면접" → INTERVIEW_1
        - "2차 면접" → INTERVIEW_2
        - "최종 면접" → INTERVIEW_FINAL
        - "최종 합격" → FINAL_OFFER
        - null if not specified

        [filters.stageResult]
        Exact enum: PASSED, FAILED, PENDING
        - "합격" → PASSED
        - "불합격" → FAILED
        - null if not specified

        [parsedText]
        For DOCUMENT_SEARCH/SUMMARY: extract skill/role keywords as Korean text
        For others: use the original question

        [Output Format — valid JSON only]
        {
          "intent": "DOCUMENT_SEARCH|SUMMARY|PATTERN_ANALYSIS|EXPERIENCE_ANALYSIS|STATISTICS|KEYWORD_SEARCH",
          "semanticRetrieval": boolean,
          "aggregation": boolean,
          "baseline": boolean,
          "parsedText": string,
          "filters": {
            "saveType": "SAVED"|"APPLY"|null,
            "stage": "DOCUMENT"|"CODING_TEST"|"INTERVIEW_1"|"INTERVIEW_2"|"INTERVIEW_FINAL"|"FINAL_OFFER"|null,
            "stageResult": "PASSED"|"FAILED"|"PENDING"|null,
            "careerType": "NEW"|"EXPERIENCED"|"BOTH"|null,
            "companyDomain": string|null,
            "techStacks": [string]|null,
            "brandName": string|null,
            "dateRangeFrom": "YYYY-MM-DD"|null,
            "dateRangeTo": "YYYY-MM-DD"|null
          }
        }

        All fields MUST be present. Unknown/unspecified values → null.
    """.trimIndent()

    fun buildRagParserPrompt(question: String): String = """
        User question: "$question"

        Parse this question according to your system instructions and return the JSON result.
    """.trimIndent()
}