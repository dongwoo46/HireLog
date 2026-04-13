package com.hirelog.api.job.domain.type

/**
 * 회사 주요 사업 도메인
 *
 * LLM이 JD 문맥에서 추출하는 값.
 * RAG aggregation/필터링 및 분석 기능에 사용된다.
 *
 * 설계 원칙:
 * - 채용 공고에 실제로 등장하는 도메인 기준으로 세분화
 * - 한 회사가 여러 도메인에 걸쳐 있어도 "주력" 도메인 하나만 선택
 * - 판단이 어려운 경우 OTHER
 */
enum class CompanyDomain(val label: String) {

    /** 결제, 뱅킹, 인슈어테크, 증권/투자 플랫폼 */
    FINTECH("핀테크"),

    /** 온라인 쇼핑, 마켓플레이스, 소셜커머스 */
    E_COMMERCE("이커머스"),

    /** 배달 앱, 음식 주문 플랫폼 */
    FOOD_DELIVERY("배달/음식"),

    /** 풀필먼트, 라스트마일 배송, B2B 물류 */
    LOGISTICS("물류/배송"),

    /** 차량공유, 킥보드, 주차, 대중교통 데이터 */
    MOBILITY("모빌리티"),

    /** 의료 AI, 원격진료, 디지털 헬스, 의약품 플랫폼 */
    HEALTHCARE("헬스케어"),

    /** 온라인 학습, LMS, 어학 플랫폼 */
    EDTECH("에듀테크"),

    /** 모바일/PC/콘솔 게임 */
    GAME("게임"),

    /** OTT, 스트리밍, 음악, 웹툰, 미디어 제작 플랫폼 */
    MEDIA_CONTENT("미디어/콘텐츠"),

    /** SNS, 데이팅앱, 온라인 커뮤니티 */
    SOCIAL_COMMUNITY("소셜/커뮤니티"),

    /** 항공, 호텔, OTA (Online Travel Agency) */
    TRAVEL_ACCOMMODATION("여행/숙박"),

    /** 부동산 플랫폼, 프롭테크 */
    REAL_ESTATE("부동산"),

    /** 채용 플랫폼, HR 관리 시스템 */
    HR_RECRUITING("HR/채용"),

    /** 애드테크, 마케팅 자동화, CRM 마케팅 */
    AD_MARKETING("광고/마케팅"),

    /** AI 솔루션, MLOps, 데이터 플랫폼 */
    AI_ML("AI/ML"),

    /** 클라우드 서비스, DevOps 툴, 인프라 플랫폼 */
    CLOUD_INFRA("클라우드/인프라"),

    /** 사이버보안, 인증, 접근제어 */
    SECURITY("보안"),

    /** ERP, CRM, B2B SaaS, 그룹웨어 */
    ENTERPRISE_SW("엔터프라이즈 SW"),

    /** 블록체인, 암호화폐, NFT, Web3 */
    BLOCKCHAIN_CRYPTO("블록체인/크립토"),

    /** 스마트팩토리, 산업 자동화, IoT */
    MANUFACTURING_IOT("제조/IoT"),

    /** 공기업, 정부 SI, 공공기관 */
    PUBLIC_SECTOR("공공"),

    /** 위 카테고리 해당 없음 */
    OTHER("기타")
}
