INSERT INTO position_category (
    name,
    normalized_name,
    status,
    description,
    created_at,
    updated_at,
    version
)
VALUES
    ('IT / Software', 'it_software', 'ACTIVE', '소프트웨어 개발 및 IT 기술 직군', now(), now(), 0),
    ('Data / AI', 'data_ai', 'ACTIVE', '데이터 분석 및 인공지능 관련 직군', now(), now(), 0),
    ('Infrastructure / Platform', 'infrastructure_platform', 'ACTIVE', '인프라 및 플랫폼 엔지니어링', now(), now(), 0),
    ('Security / Quality', 'security_quality', 'ACTIVE', '보안 및 품질 관련 직군', now(), now(), 0),
    ('IT Planning / Support', 'it_planning_support', 'ACTIVE', 'IT 기획 및 기술 지원', now(), now(), 0),
    ('Product / Planning', 'product_planning', 'ACTIVE', '프로덕트 및 서비스 기획', now(), now(), 0),
    ('Business / Strategy', 'business_strategy', 'ACTIVE', '경영 및 전략', now(), now(), 0),
    ('HR', 'hr', 'ACTIVE', '인사 및 채용', now(), now(), 0),
    ('Finance / Accounting', 'finance_accounting', 'ACTIVE', '재무 및 회계', now(), now(), 0),
    ('Marketing', 'marketing', 'ACTIVE', '마케팅 직군', now(), now(), 0),
    ('Sales', 'sales', 'ACTIVE', '영업 직군', now(), now(), 0),
    ('Design', 'design', 'ACTIVE', '디자인 직군', now(), now(), 0),
    ('Content', 'content', 'ACTIVE', '콘텐츠 제작', now(), now(), 0),
    ('Operations / Admin', 'operations_admin', 'ACTIVE', '운영 및 사무', now(), now(), 0),
    ('Manufacturing / Engineering', 'manufacturing_engineering', 'ACTIVE', '제조 및 엔지니어링', now(), now(), 0),
    ('Logistics', 'logistics', 'ACTIVE', '물류 및 유통', now(), now(), 0),
    ('Service / Hospitality', 'service_hospitality', 'ACTIVE', '서비스 및 외식/관광', now(), now(), 0);

INSERT INTO position (
    name, normalized_name, status, category_id,
    created_at, updated_at, version
)
VALUES
    ('Backend Engineer', 'backend_engineer', 'ACTIVE',
     (SELECT id FROM position_category WHERE normalized_name='it_software'),
     now(), now(), 0),

    ('Frontend Engineer', 'frontend_engineer', 'ACTIVE',
     (SELECT id FROM position_category WHERE normalized_name='it_software'),
     now(), now(), 0),

    ('Fullstack Engineer', 'fullstack_engineer', 'ACTIVE',
     (SELECT id FROM position_category WHERE normalized_name='it_software'),
     now(), now(), 0),

    ('Mobile App Engineer', 'mobile_app_engineer', 'ACTIVE',
     (SELECT id FROM position_category WHERE normalized_name='it_software'),
     now(), now(), 0),

    ('Game Developer', 'game_developer', 'ACTIVE',
     (SELECT id FROM position_category WHERE normalized_name='it_software'),
     now(), now(), 0),

    ('System Engineer', 'system_engineer', 'ACTIVE',
     (SELECT id FROM position_category WHERE normalized_name='it_software'),
     now(), now(), 0),

    ('Embedded Engineer', 'embedded_engineer', 'ACTIVE',
     (SELECT id FROM position_category WHERE normalized_name='it_software'),
     now(), now(), 0);

INSERT INTO position (
    name, normalized_name, status, category_id,
    created_at, updated_at, version
)
VALUES
    ('Data Engineer', 'data_engineer', 'ACTIVE',
     (SELECT id FROM position_category WHERE normalized_name='data_ai'),
     now(), now(), 0),

    ('Data Analyst', 'data_analyst', 'ACTIVE',
     (SELECT id FROM position_category WHERE normalized_name='data_ai'),
     now(), now(), 0),

    ('Data Scientist', 'data_scientist', 'ACTIVE',
     (SELECT id FROM position_category WHERE normalized_name='data_ai'),
     now(), now(), 0),

    ('Machine Learning Engineer', 'machine_learning_engineer', 'ACTIVE',
     (SELECT id FROM position_category WHERE normalized_name='data_ai'),
     now(), now(), 0),

    ('AI Engineer', 'ai_engineer', 'ACTIVE',
     (SELECT id FROM position_category WHERE normalized_name='data_ai'),
     now(), now(), 0);

INSERT INTO position (
    name, normalized_name, status, category_id,
    created_at, updated_at, version
)
VALUES
    ('DevOps Engineer', 'devops_engineer', 'ACTIVE',
     (SELECT id FROM position_category WHERE normalized_name='infrastructure_platform'),
     now(), now(), 0),

    ('Site Reliability Engineer', 'site_reliability_engineer', 'ACTIVE',
     (SELECT id FROM position_category WHERE normalized_name='infrastructure_platform'),
     now(), now(), 0),

    ('Platform Engineer', 'platform_engineer', 'ACTIVE',
     (SELECT id FROM position_category WHERE normalized_name='infrastructure_platform'),
     now(), now(), 0),

    ('Cloud Engineer', 'cloud_engineer', 'ACTIVE',
     (SELECT id FROM position_category WHERE normalized_name='infrastructure_platform'),
     now(), now(), 0);

INSERT INTO position (
    name, normalized_name, status, category_id,
    created_at, updated_at, version
)
VALUES
    ('Security Engineer', 'security_engineer', 'ACTIVE',
     (SELECT id FROM position_category WHERE normalized_name='security_quality'),
     now(), now(), 0),

    ('QA Engineer', 'qa_engineer', 'ACTIVE',
     (SELECT id FROM position_category WHERE normalized_name='security_quality'),
     now(), now(), 0),

    ('Test Automation Engineer', 'test_automation_engineer', 'ACTIVE',
     (SELECT id FROM position_category WHERE normalized_name='security_quality'),
     now(), now(), 0);

INSERT INTO position (
    name, normalized_name, status, category_id,
    created_at, updated_at, version
)
VALUES
    ('IT Planner', 'it_planner', 'ACTIVE',
     (SELECT id FROM position_category WHERE normalized_name='it_planning_support'),
     now(), now(), 0),

    ('Solutions Engineer', 'solutions_engineer', 'ACTIVE',
     (SELECT id FROM position_category WHERE normalized_name='it_planning_support'),
     now(), now(), 0),

    ('Technical Support Engineer', 'technical_support_engineer', 'ACTIVE',
     (SELECT id FROM position_category WHERE normalized_name='it_planning_support'),
     now(), now(), 0),

    ('Network Engineer', 'network_engineer', 'ACTIVE',
     (SELECT id FROM position_category WHERE normalized_name='it_planning_support'),
     now(), now(), 0),

    ('Database Administrator', 'database_administrator', 'ACTIVE',
     (SELECT id FROM position_category WHERE normalized_name='it_planning_support'),
     now(), now(), 0);

INSERT INTO position (
    name, normalized_name, status, category_id,
    created_at, updated_at, version
)
VALUES
    ('Product Manager', 'product_manager', 'ACTIVE',
     (SELECT id FROM position_category WHERE normalized_name='product_planning'),
     now(), now(), 0),

    ('Product Owner', 'product_owner', 'ACTIVE',
     (SELECT id FROM position_category WHERE normalized_name='product_planning'),
     now(), now(), 0),

    ('Project Manager', 'project_manager', 'ACTIVE',
     (SELECT id FROM position_category WHERE normalized_name='product_planning'),
     now(), now(), 0),

    ('Service Planner', 'service_planner', 'ACTIVE',
     (SELECT id FROM position_category WHERE normalized_name='product_planning'),
     now(), now(), 0),

    ('Business Planner', 'business_planner', 'ACTIVE',
     (SELECT id FROM position_category WHERE normalized_name='product_planning'),
     now(), now(), 0);

INSERT INTO position (
    name, normalized_name, status, category_id,
    created_at, updated_at, version
)
VALUES
    ('Business Manager', 'business_manager', 'ACTIVE',
     (SELECT id FROM position_category WHERE normalized_name='business_strategy'),
     now(), now(), 0),

    ('Strategy Manager', 'strategy_manager', 'ACTIVE',
     (SELECT id FROM position_category WHERE normalized_name='business_strategy'),
     now(), now(), 0),

    ('HR Manager', 'hr_manager', 'ACTIVE',
     (SELECT id FROM position_category WHERE normalized_name='hr'),
     now(), now(), 0),

    ('HR Specialist', 'hr_specialist', 'ACTIVE',
     (SELECT id FROM position_category WHERE normalized_name='hr'),
     now(), now(), 0),

    ('Recruiter', 'recruiter', 'ACTIVE',
     (SELECT id FROM position_category WHERE normalized_name='hr'),
     now(), now(), 0),

    ('Accountant', 'accountant', 'ACTIVE',
     (SELECT id FROM position_category WHERE normalized_name='finance_accounting'),
     now(), now(), 0),

    ('Financial Analyst', 'financial_analyst', 'ACTIVE',
     (SELECT id FROM position_category WHERE normalized_name='finance_accounting'),
     now(), now(), 0),

    ('Finance Manager', 'finance_manager', 'ACTIVE',
     (SELECT id FROM position_category WHERE normalized_name='finance_accounting'),
     now(), now(), 0);


-- 기본 카테고리 (예: GENERAL)
INSERT INTO position_category (name, normalized_name, created_at, updated_at)
VALUES ('GENERAL', 'general', now(), now())
    ON CONFLICT (normalized_name) DO NOTHING;

INSERT INTO position (
    name,
    normalized_name,
    status,
    category_id,
    description,
    created_at,
    updated_at,
    version
)
SELECT
    'UNKNOWN',
    'unknown',
    'ACTIVE',
    pc.id,
    'LLM 매핑 실패 시 fallback 포지션',
    now(),
    now(),
    0
FROM position_category pc
WHERE pc.normalized_name = 'general'
    ON CONFLICT (normalized_name) DO NOTHING;
