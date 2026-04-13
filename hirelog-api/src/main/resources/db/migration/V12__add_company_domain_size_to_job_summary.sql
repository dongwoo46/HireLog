-- RAG aggregation/필터링용 구조화 필드 추가
-- companyDomain: LLM 추출 회사 도메인 enum (CompanyDomain), 기본값 OTHER
-- companySize:   LLM 추출 회사 규모 enum (CompanySize), 기본값 UNKNOWN

ALTER TABLE job_summary ADD COLUMN company_domain VARCHAR(30) NOT NULL DEFAULT 'OTHER';
ALTER TABLE job_summary ADD COLUMN company_size   VARCHAR(20) NOT NULL DEFAULT 'UNKNOWN';
